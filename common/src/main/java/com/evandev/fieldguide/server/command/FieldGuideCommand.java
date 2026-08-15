package com.evandev.fieldguide.server.command;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.api.Category;
import com.evandev.fieldguide.api.EntryVariantData;
import com.evandev.fieldguide.api.GuideEntry;
import com.evandev.fieldguide.api.variant.VariantDef;
import com.evandev.fieldguide.compat.cobblemon.FieldGuideCobblemonCompat;
import com.evandev.fieldguide.entry.EntryResolver;
import com.evandev.fieldguide.network.ExportContentPacket;
import com.evandev.fieldguide.platform.Services;
import com.evandev.fieldguide.server.ServerFieldGuideManager;
import com.evandev.fieldguide.server.progress.FieldGuideProgressManager;
import com.evandev.fieldguide.server.progress.PlayerFieldGuideProgress;
import com.evandev.fieldguide.variant.FieldGuideVariantManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class FieldGuideCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fieldguide")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("export")
                        .then(Commands.literal("names").executes(ctx -> export(ctx.getSource(), "names")))
                        .then(Commands.literal("descriptions").executes(ctx -> export(ctx.getSource(), "descriptions")))
                        .then(Commands.literal("all").executes(ctx -> export(ctx.getSource(), "all")))
                        .then(Commands.literal("missing").executes(ctx -> export(ctx.getSource(), "missing")))
                        .then(Commands.literal("feature")
                                .then(Commands.argument("feature", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                ctx.getSource().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).keySet(), builder))

                                        .executes(ctx -> exportFeature(ctx.getSource(), ResourceLocationArgument.getId(ctx, "feature"), Blocks.DIRT))

                                        .then(Commands.argument("base_block", ResourceLocationArgument.id())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                        BuiltInRegistries.BLOCK.keySet(), builder))
                                                .executes(ctx -> exportFeature(
                                                        ctx.getSource(),
                                                        ResourceLocationArgument.getId(ctx, "feature"),
                                                        BuiltInRegistries.BLOCK.get(EntryResolver.getRawId(ResourceLocationArgument.getId(ctx, "base_block")))
                                                ))
                                        )
                                )
                        )
                )
                .then(Commands.literal("reload").executes(ctx -> reload(ctx.getSource())))
                .then(Commands.literal("grant")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.literal("everything")
                                        .executes(ctx -> grantEverything(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))
                                )
                                .then(Commands.literal("category")
                                        .then(Commands.argument("category", ResourceLocationArgument.id())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(ServerFieldGuideManager.getInstance().getCategories().keySet(), builder))
                                                .executes(ctx -> grantCategory(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), ResourceLocationArgument.getId(ctx, "category")))
                                        )
                                )
                                .then(Commands.literal("only")
                                        .then(Commands.argument("entry", ResourceLocationArgument.id())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                        ServerFieldGuideManager.getInstance().getAllEntryIds(),
                                                        builder))
                                                .executes(ctx -> grantEntry(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), ResourceLocationArgument.getId(ctx, "entry")))
                                        )
                                )
                                .then(Commands.literal("variant")
                                        .then(Commands.argument("entry", ResourceLocationArgument.id())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                        ServerFieldGuideManager.getInstance().getAllEntryIds(),
                                                        builder))
                                                .then(Commands.argument("variant", StringArgumentType.string())
                                                        .suggests((ctx, builder) -> {
                                                            ResourceLocation entryId = ResourceLocationArgument.getId(ctx, "entry");
                                                            List<String> variants = new ArrayList<>();
                                                            variants.add("all");

                                                            if (Services.PLATFORM.isModLoaded("cobblemon") && entryId.getPath().contains("cobblemon")) {
                                                                variants.addAll(FieldGuideCobblemonCompat.getVariantIds(entryId));
                                                            } else {
                                                                ResourceLocation rawId = EntryResolver.getRawId(entryId);
                                                                if (BuiltInRegistries.ENTITY_TYPE.containsKey(rawId)) {
                                                                    EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rawId);
                                                                    variants.addAll(FieldGuideVariantManager.getVariantIds(type, ctx.getSource().getLevel()));
                                                                }
                                                            }
                                                            return SharedSuggestionProvider.suggest(variants, builder);
                                                        })
                                                        .executes(ctx -> grantVariant(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), ResourceLocationArgument.getId(ctx, "entry"), StringArgumentType.getString(ctx, "variant")))
                                                )
                                        )
                                )
                        )
                )
                .then(Commands.literal("revoke")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.literal("everything")
                                        .executes(ctx -> revokeEverything(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))
                                )
                                .then(Commands.literal("category")
                                        .then(Commands.argument("category", ResourceLocationArgument.id())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(ServerFieldGuideManager.getInstance().getCategories().keySet(), builder))
                                                .executes(ctx -> revokeCategory(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), ResourceLocationArgument.getId(ctx, "category")))
                                        )
                                )
                                .then(Commands.literal("only")
                                        .then(Commands.argument("entry", ResourceLocationArgument.id())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                        ServerFieldGuideManager.getInstance().getAllEntryIds(),
                                                        builder))
                                                .executes(ctx -> revokeEntry(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), ResourceLocationArgument.getId(ctx, "entry")))
                                        )
                                )
                                .then(Commands.literal("variant")
                                        .then(Commands.argument("entry", ResourceLocationArgument.id())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                        ServerFieldGuideManager.getInstance().getAllEntryIds(),
                                                        builder))
                                                .then(Commands.argument("variant", StringArgumentType.string())
                                                        .suggests((ctx, builder) -> {
                                                            ResourceLocation entryId = ResourceLocationArgument.getId(ctx, "entry");
                                                            List<String> variants = new ArrayList<>();
                                                            variants.add("all");

                                                            if (Services.PLATFORM.isModLoaded("cobblemon") && entryId.getPath().contains("cobblemon")) {
                                                                variants.addAll(FieldGuideCobblemonCompat.getVariantIds(entryId));
                                                            } else {
                                                                ResourceLocation rawId = EntryResolver.getRawId(entryId);
                                                                if (BuiltInRegistries.ENTITY_TYPE.containsKey(rawId)) {
                                                                    EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rawId);
                                                                    variants.addAll(FieldGuideVariantManager.getVariantIds(type, ctx.getSource().getLevel()));
                                                                }
                                                            }
                                                            return SharedSuggestionProvider.suggest(variants, builder);
                                                        })
                                                        .executes(ctx -> revokeVariant(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), ResourceLocationArgument.getId(ctx, "entry"), StringArgumentType.getString(ctx, "variant")))
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static int grantVariant(CommandSourceStack source, Collection<ServerPlayer> targets, ResourceLocation entryId, String variantId) {
        ResourceLocation canonicalId = ServerFieldGuideManager.getInstance().resolveCanonicalEntryId(entryId);
        ResourceLocation targetEntryId = canonicalId != null ? canonicalId : entryId;
        FieldGuideProgressManager manager = FieldGuideProgressManager.getInstance();

        if (variantId.equalsIgnoreCase("all")) {
            for (ServerPlayer player : targets) {
                PlayerFieldGuideProgress progress = manager.getProgress(player);
                if (progress != null) {
                    progress.unlock(player, targetEntryId, null, true);
                    unlockVariants(player, targetEntryId, source.getLevel());
                }
            }
            source.sendSuccess(() -> Component.translatable("commands.fieldguide.grant.variant.success", "all", targetEntryId.toString()), true);
            return targets.size();
        }

        for (ServerPlayer player : targets) {
            PlayerFieldGuideProgress progress = manager.getProgress(player);
            if (progress != null) {
                progress.unlock(player, targetEntryId, variantId, true);
            }
        }
        source.sendSuccess(() -> Component.translatable("commands.fieldguide.grant.variant.success", variantId, targetEntryId.toString()), true);
        return targets.size();
    }

    private static int revokeVariant(CommandSourceStack source, Collection<ServerPlayer> targets, ResourceLocation entryId, String variantId) {
        ResourceLocation canonicalId = ServerFieldGuideManager.getInstance().resolveCanonicalEntryId(entryId);
        ResourceLocation targetEntryId = canonicalId != null ? canonicalId : entryId;
        FieldGuideProgressManager manager = FieldGuideProgressManager.getInstance();

        if (variantId.equalsIgnoreCase("all")) {
            if (Services.PLATFORM.isModLoaded("cobblemon") && targetEntryId.getPath().contains("cobblemon")) {
                List<String> variants = FieldGuideCobblemonCompat.getVariantIds(targetEntryId);
                if (!variants.isEmpty()) {
                    for (ServerPlayer player : targets) {
                        PlayerFieldGuideProgress progress = manager.getProgress(player);
                        if (progress != null) {
                            for (String def : variants) {
                                progress.revoke(targetEntryId + "#" + def);
                            }
                        }
                    }
                    source.sendSuccess(() -> Component.translatable("commands.fieldguide.revoke.variant.success", "all", targetEntryId.toString()), true);
                    return targets.size();
                }
            } else {
                ResourceLocation rawId = EntryResolver.getRawId(targetEntryId);
                if (BuiltInRegistries.ENTITY_TYPE.containsKey(rawId)) {
                    EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rawId);
                    List<VariantDef> variants = FieldGuideVariantManager.getVariants(type, source.getLevel());
                    if (!variants.isEmpty()) {
                        for (ServerPlayer player : targets) {
                            PlayerFieldGuideProgress progress = manager.getProgress(player);
                            if (progress != null) {
                                for (VariantDef def : variants) {
                                    progress.revoke(targetEntryId.toString() + "#" + def.id());
                                }
                            }
                        }
                        source.sendSuccess(() -> Component.translatable("commands.fieldguide.revoke.variant.success", "all", targetEntryId.toString()), true);
                        return targets.size();
                    }
                }
            }
        }

        String fullId = targetEntryId.toString() + "#" + variantId;
        for (ServerPlayer player : targets) {
            PlayerFieldGuideProgress progress = manager.getProgress(player);
            if (progress != null) {
                progress.revoke(fullId);
            }
        }
        source.sendSuccess(() -> Component.translatable("commands.fieldguide.revoke.variant.success", variantId, targetEntryId.toString()), true);
        return targets.size();
    }

    private static int grantEverything(CommandSourceStack source, Collection<ServerPlayer> targets) {
        FieldGuideProgressManager manager = FieldGuideProgressManager.getInstance();
        Set<ResourceLocation> allEntries = ServerFieldGuideManager.getInstance().getAllEntryIds();
        for (ServerPlayer player : targets) {
            PlayerFieldGuideProgress progress = manager.getProgress(player);
            if (progress != null) {
                for (ResourceLocation entryId : allEntries) {
                    progress.unlock(player, entryId, null, true);
                    unlockVariants(player, entryId, source.getLevel());
                }
            }
        }
        source.sendSuccess(() -> Component.translatable("commands.fieldguide.grant.everything.success", targets.size()), true);
        return targets.size();
    }

    private static void unlockVariants(ServerPlayer player, ResourceLocation entryId, ServerLevel level) {
        PlayerFieldGuideProgress progress = FieldGuideProgressManager.getInstance().getProgress(player);
        if (progress == null) return;

        if (Services.PLATFORM.isModLoaded("cobblemon") && entryId.getPath().contains("cobblemon")) {
            for (String variantId : FieldGuideCobblemonCompat.getVariantIds(entryId)) {
                progress.unlock(player, entryId, variantId, true);
            }
        } else {
            ResourceLocation rawId = EntryResolver.getRawId(entryId);

            if (BuiltInRegistries.ENTITY_TYPE.containsKey(rawId)) {
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rawId);
                List<VariantDef> variants = FieldGuideVariantManager.getVariants(type, level);
                for (VariantDef variant : variants) {
                    progress.unlock(player, entryId, variant.id(), true);
                }
            }

            GuideEntry resolved = ServerFieldGuideManager.getInstance().getResolvedEntry(entryId);
            if (resolved != null && resolved.visualVariants() != null) {
                for (EntryVariantData vd : resolved.visualVariants()) {
                    progress.unlock(player, entryId, vd.variantId(), true);
                }
            }
        }
    }

    private static void revokeVariants(ServerPlayer player, ResourceLocation entryId, ServerLevel level) {
        PlayerFieldGuideProgress progress = FieldGuideProgressManager.getInstance().getProgress(player);
        if (progress == null) return;

        if (Services.PLATFORM.isModLoaded("cobblemon") && entryId.getPath().contains("cobblemon")) {
            for (String variantId : FieldGuideCobblemonCompat.getVariantIds(entryId)) {
                progress.revoke(entryId + "#" + variantId);
            }
        } else {
            ResourceLocation rawId = EntryResolver.getRawId(entryId);

            if (BuiltInRegistries.ENTITY_TYPE.containsKey(rawId)) {
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rawId);
                List<VariantDef> variants = FieldGuideVariantManager.getVariants(type, level);
                for (VariantDef variant : variants) {
                    progress.revoke(entryId + "#" + variant.id());
                }
            }

            GuideEntry resolved = ServerFieldGuideManager.getInstance().getResolvedEntry(entryId);
            if (resolved != null && resolved.visualVariants() != null) {
                for (EntryVariantData vd : resolved.visualVariants()) {
                    progress.revoke(entryId + "#" + vd.variantId());
                }
            }
        }
    }

    private static int grantCategory(CommandSourceStack source, Collection<ServerPlayer> targets, ResourceLocation categoryId) {
        Category category = ServerFieldGuideManager.getInstance().getCategories().get(categoryId);
        if (category == null) {
            source.sendFailure(Component.translatable("commands.fieldguide.category.not_found", categoryId.toString()));
            return 0;
        }
        FieldGuideProgressManager manager = FieldGuideProgressManager.getInstance();
        Set<ResourceLocation> entryIds = ServerFieldGuideManager.getInstance().getEntryIdsForCategory(categoryId);
        for (ServerPlayer player : targets) {
            PlayerFieldGuideProgress progress = manager.getProgress(player);
            if (progress != null) {
                for (ResourceLocation entryId : entryIds) {
                    progress.unlock(player, entryId, null, true);
                    unlockVariants(player, entryId, source.getLevel());
                }
            }
        }
        source.sendSuccess(() -> Component.translatable("commands.fieldguide.grant.category.success", categoryId.toString(), targets.size()), true);
        return targets.size();
    }

    private static int export(CommandSourceStack source, String type) {
        if (source.getEntity() instanceof ServerPlayer player) {
            Services.NETWORK.sendToPlayer(new ExportContentPacket(type), player);
            source.sendSuccess(() -> Component.literal("Triggering export on client..."), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("This command must be run by an in-game player."));
            return 0;
        }
    }

    private static int exportFeature(CommandSourceStack source, ResourceLocation featureId, Block baseBlock) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command must be run by an in-game player."));
            return 0;
        }

        ServerLevel level = source.getLevel();
        int y = Math.min(player.getBlockY() + 50, level.getMaxBuildHeight() - 40);
        BlockPos origin = new BlockPos(player.getBlockX(), y, player.getBlockZ());

        Registry<ConfiguredFeature<?, ?>> registry = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        ConfiguredFeature<?, ?> feature = registry.get(featureId);

        if (feature == null) {
            source.sendFailure(Component.literal("Feature not found: " + featureId.toString()));
            return 0;
        }

        int radiusH = 16;
        int radiusV = 32;

        for (int dx = -radiusH; dx <= radiusH; dx++) {
            for (int dy = -2; dy <= radiusV; dy++) {
                for (int dz = -radiusH; dz <= radiusH; dz++) {
                    level.setBlock(origin.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        level.setBlock(origin.below(), baseBlock.defaultBlockState(), 3);

        feature.place(level, level.getChunkSource().getGenerator(), level.getRandom(), origin);

        BlockPos min = null;
        BlockPos max = null;

        for (int dx = -radiusH; dx <= radiusH; dx++) {
            for (int dy = -2; dy <= radiusV; dy++) {
                for (int dz = -radiusH; dz <= radiusH; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!level.getBlockState(pos).isAir() && !pos.equals(origin.below())) {
                        if (min == null) {
                            min = pos;
                            max = pos;
                        } else {
                            min = new BlockPos(Math.min(min.getX(), pos.getX()), Math.min(min.getY(), pos.getY()), Math.min(min.getZ(), pos.getZ()));
                            max = new BlockPos(Math.max(max.getX(), pos.getX()), Math.max(max.getY(), pos.getY()), Math.max(max.getZ(), pos.getZ()));
                        }
                    }
                }
            }
        }

        if (min == null) {
            source.sendFailure(Component.literal("Feature generated no blocks! It might require a different base block."));
        } else {
            StructureTemplate template = new StructureTemplate();
            BlockPos size = max.subtract(min).offset(1, 1, 1);
            template.fillFromWorld(level, min, size, false, Blocks.AIR);

            try {
                Path exportDir = Services.PLATFORM.getConfigDirectory().getParent()
                        .resolve("fieldguide_exports")
                        .resolve("assets")
                        .resolve(Constants.MOD_ID)
                        .resolve("structures");
                Path filePath = exportDir.resolve(featureId.getPath() + ".nbt");
                Files.createDirectories(filePath.getParent());

                try (OutputStream out = Files.newOutputStream(filePath)) {
                    CompoundTag tag = template.save(new CompoundTag());
                    NbtIo.writeCompressed(tag, out);
                    source.sendSuccess(() -> Component.literal("§aExported feature to " + filePath.toAbsolutePath()), true);
                }
            } catch (Exception e) {
                source.sendFailure(Component.literal("Failed to save feature: " + e.getMessage()));
            }
        }

        for (int dx = -radiusH; dx <= radiusH; dx++) {
            for (int dy = -2; dy <= radiusV; dy++) {
                for (int dz = -radiusH; dz <= radiusH; dz++) {
                    level.setBlock(origin.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        return 1;
    }

    private static int reload(CommandSourceStack source) {
        ServerFieldGuideManager.getInstance().reload(source.getServer());
        source.sendSuccess(() -> Component.literal("FieldGuide configuration and caches reloaded!"), true);
        return 1;
    }

    private static int grantEntry(CommandSourceStack source, Collection<ServerPlayer> targets, ResourceLocation entryId) {
        ResourceLocation canonicalId = ServerFieldGuideManager.getInstance().resolveCanonicalEntryId(entryId);
        ResourceLocation targetEntryId = canonicalId != null ? canonicalId : entryId;
        FieldGuideProgressManager manager = FieldGuideProgressManager.getInstance();
        for (ServerPlayer player : targets) {
            PlayerFieldGuideProgress progress = manager.getProgress(player);
            if (progress != null) {
                progress.unlock(player, targetEntryId, null, true);
                unlockVariants(player, targetEntryId, source.getLevel());
            }
        }
        source.sendSuccess(() -> Component.translatable("commands.fieldguide.grant.entry.success", targetEntryId.toString()), true);
        return targets.size();
    }

    private static int revokeEverything(CommandSourceStack source, Collection<ServerPlayer> targets) {
        FieldGuideProgressManager manager = FieldGuideProgressManager.getInstance();
        for (ServerPlayer player : targets) {
            PlayerFieldGuideProgress progress = manager.getProgress(player);
            if (progress != null) {
                progress.revokeAll();
            }
        }
        source.sendSuccess(() -> Component.translatable("commands.fieldguide.revoke.everything.success"), true);
        return targets.size();
    }

    private static int revokeCategory(CommandSourceStack source, Collection<ServerPlayer> targets, ResourceLocation categoryId) {
        FieldGuideProgressManager manager = FieldGuideProgressManager.getInstance();
        Set<ResourceLocation> entryIds = ServerFieldGuideManager.getInstance().getEntryIdsForCategory(categoryId);
        for (ServerPlayer player : targets) {
            PlayerFieldGuideProgress progress = manager.getProgress(player);
            if (progress != null) {
                for (ResourceLocation entryId : entryIds) {
                    progress.revoke(entryId.toString());
                    revokeVariants(player, entryId, source.getLevel());
                }
            }
        }
        source.sendSuccess(() -> Component.translatable("commands.fieldguide.revoke.category.success", categoryId.toString(), targets.size()), true);
        return targets.size();
    }

    private static int revokeEntry(CommandSourceStack source, Collection<ServerPlayer> targets, ResourceLocation entryId) {
        ResourceLocation canonicalId = ServerFieldGuideManager.getInstance().resolveCanonicalEntryId(entryId);
        ResourceLocation targetEntryId = canonicalId != null ? canonicalId : entryId;
        FieldGuideProgressManager manager = FieldGuideProgressManager.getInstance();
        for (ServerPlayer player : targets) {
            PlayerFieldGuideProgress progress = manager.getProgress(player);
            if (progress != null) {
                progress.revoke(targetEntryId.toString());
                revokeVariants(player, targetEntryId, source.getLevel());
            }
        }
        source.sendSuccess(() -> Component.translatable("commands.fieldguide.revoke.entry.success", targetEntryId.toString()), true);
        return targets.size();
    }
}