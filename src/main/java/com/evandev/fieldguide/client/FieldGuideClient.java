package com.evandev.fieldguide.client;

import com.evandev.fieldguide.Constants;
import com.evandev.fieldguide.api.Category;
import com.evandev.fieldguide.api.attribute.AttributeRegistry;
import com.evandev.fieldguide.client.attribute.DefaultAttributeProvider;
import com.evandev.fieldguide.client.gui.screens.BookScreen;
import com.evandev.fieldguide.client.gui.screens.FieldGuideCategoryScreen;
import com.evandev.fieldguide.client.gui.screens.FieldGuideEntryScreen;
import com.evandev.fieldguide.client.gui.util.GuiCompat;
import com.evandev.fieldguide.client.scan.FieldGuideScanner;
import com.evandev.fieldguide.compat.SeasonsCompat;
import com.evandev.fieldguide.compat.cobblemon.ClientFieldGuideCobblemonCompat;
import com.evandev.fieldguide.compat.cobblemon.FieldGuideCobblemonCompat;
import com.evandev.fieldguide.config.ClientConfig;
import com.evandev.fieldguide.config.ServerConfig;
import com.evandev.fieldguide.item.ModItems;
import com.evandev.fieldguide.mixin.accessor.MobAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

//? if <26.1 {
import com.mojang.blaze3d.systems.RenderSystem;
//?}

//? if >=26.1 {
/*import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
*///?}

public class FieldGuideClient {
    //? if >=26.1 {
    /*public static final KeyMapping.Category FIELD_GUIDE_CATEGORY = KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "main"));
    *///?}
    private static final long AUTO_OPEN_THRESHOLD_MS = 5000;
    public static KeyMapping OPEN_GUIDE_KEY;
    public static KeyMapping SCAN_KEY;

    public static void init() {
        SeasonsCompat.init();
        AttributeRegistry.register(new DefaultAttributeProvider());

        OPEN_GUIDE_KEY = new KeyMapping(
                "key.fieldguide.open",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_B,
                //? if <26.1 {
                "category.fieldguide.main"
                //?} else {
                /*FIELD_GUIDE_CATEGORY
                *///?}
        );

        SCAN_KEY = new KeyMapping(
                "key.fieldguide.scan",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                //? if <26.1 {
                "category.fieldguide.main"
                //?} else {
                /*FIELD_GUIDE_CATEGORY
                *///?}
        );
    }

    public static void playMobCry(Entity entity) {
        if (FieldGuideCobblemonCompat.isPokemon(entity)) {
            ClientFieldGuideCobblemonCompat.playPokemonCry(entity);
            return;
        }

        if (entity instanceof Mob mob) {
            SoundEvent sound = ((MobAccessor) mob).fieldguide$callGetAmbientSound();
            if (sound != null) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F, 1.5F));
            }
        }
    }

    public static void onClientTick() {
        if (OPEN_GUIDE_KEY != null && OPEN_GUIDE_KEY.consumeClick()) {
            openGuide();
        }
    }

    public static boolean canOpenGuide() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return false;

        if (ServerConfig.get().requireItemToOpen) {
            boolean hasItem = false;
            for (int i = 0; i < minecraft.player.getInventory().getContainerSize(); i++) {
                if (ModItems.FIELD_GUIDE != null && minecraft.player.getInventory().getItem(i).getItem() == ModItems.FIELD_GUIDE.get()) {
                    hasItem = true;
                    break;
                }
            }
            return hasItem;
        }
        return true;
    }

    public static void openGuide() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null && minecraft.player != null) {
            if (!canOpenGuide()) return;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F));
            ClientFieldGuideManager manager = ClientFieldGuideManager.getInstance();
            long lastTime = manager.getLastUnlockTime();
            Object lastEntry = manager.getLastUnlockedEntry();
            String lastVariant = manager.getLastUnlockedVariant();

            boolean isRecent = (System.currentTimeMillis() - lastTime) < AUTO_OPEN_THRESHOLD_MS;
            if (isRecent && lastEntry != null) {
                Category targetCategory = manager.getCategoryForEntry(lastEntry);
                if (targetCategory != null) {
                    int page = FieldGuideCategoryScreen.getPageForEntry(targetCategory, lastEntry);
                    FieldGuideCategoryScreen mainScreen = new FieldGuideCategoryScreen(targetCategory, page);
                    FieldGuideEntryScreen entryScreen = new FieldGuideEntryScreen(mainScreen, lastEntry);

                    if (lastVariant != null) {
                        entryScreen.setInitialVariant(lastVariant);
                    }

                    minecraft.setScreen(entryScreen);
                    return;
                }
            }

            openDefaultScreen();
        }
    }

    public static void openDefaultScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if ("last_opened_screen".equals(ClientConfig.get().defaultScreen) && BookScreen.lastOpenedScreen != null) {
            minecraft.setScreen(BookScreen.lastOpenedScreen);
        } else {
            minecraft.setScreen(new FieldGuideCategoryScreen());
        }
    }

    public static void renderScanningIcon(GuiGraphics guiGraphics, float partialTick) {
        ClientConfig config = ClientConfig.get();
        if (!config.showScanIcon) return;

        FieldGuideScanner scanner = FieldGuideScanner.getInstance();
        boolean outOfRange = scanner.getOutOfRangeTarget() != null;
        boolean isFading = scanner.getFadingTarget() != null;
        boolean hasTarget = scanner.getScanningTarget() != null;

        if (hasTarget || isFading || outOfRange) {
            int screenWidth = guiGraphics.guiWidth();
            int screenHeight = guiGraphics.guiHeight();
            int textureSize = 32;

            int xOffset = config.scanIconXOffset;
            int x = ((screenWidth - textureSize) / 2) + xOffset;
            int yOffset = config.scanIconYOffset;
            int y = ((screenHeight - textureSize) / 2) - yOffset;

            int frame;
            int totalFramesInTexture = 6;

            if (outOfRange) {
                frame = 5;
            } else if (isFading) {
                frame = 4;
            } else {
                float progress = scanner.getScanProgress(partialTick);
                if (scanner.getIsTickingDown()) return;
                int animationFrames = 4;
                frame = (int) Math.min(Math.floor(progress * animationFrames), animationFrames - 1);
            }

            GuiCompat.push(guiGraphics);
            GuiCompat.translate(guiGraphics, 0, 0, 100);
            //? if <26.1 {
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableBlend();
            //?}

            GuiCompat.blit(guiGraphics, Constants.SCANNING_ICON_TEXTURE, x, y, 0, textureSize * frame, textureSize, textureSize, textureSize, textureSize * totalFramesInTexture);

            //? if <26.1 {
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            //?}
            GuiCompat.pop(guiGraphics);
        }
    }
}
