package com.evandev.fieldguide;

import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class Constants {
    public static final String MOD_ID = "fieldguide";
    public static final String MOD_NAME = "FieldGuide";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    public static final Type LIST_STRING_TYPE = new TypeToken<List<String>>() {
    }.getType();

    public static final Type MAP_STRING_LIST_STRING_TYPE = new TypeToken<Map<String, List<String>>>() {
    }.getType();

    // Backgrounds
    public static final ResourceLocation BOOK_TEXTURE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/book.png");
    public static final ResourceLocation TITLE_PAGE_TEXTURE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/page_title.png");
    public static final ResourceLocation LIST_PAGE_TEXTURE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/page_list.png");
    public static final ResourceLocation DETAILS_PAGE_TEXTURE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/page_details.png");
    public static final ResourceLocation DETAILS_PAGE_V_TEXTURE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/page_details_v.png");
    public static final ResourceLocation DETAILS_PAGE_A_TEXTURE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/page_details_a.png");
    public static final ResourceLocation DETAILS_PAGE_VA_TEXTURE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/page_details_va.png");
    public static final ResourceLocation TOAST_TEXTURE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/toast.png");
    public static final ResourceLocation JOURNAL_TITLE_PAGE_TEXTURE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/page_journal_title.png");
    public static final ResourceLocation JOURNAL_PAGE_TEXTURE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/page_journal.png");
    public static final ResourceLocation VARIANT_WIDGET_TEXTURE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/variant_overview_bg.png");

    // Sounds
    public static final ResourceLocation ITEM_PICKUP_SOUND = ResourceLocation.fromNamespaceAndPath("minecraft", "entity.item.pickup");

    // Elements
    public static final ResourceLocation WIDGETS_TEXTURE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/widgets.png");
    public static final ResourceLocation LIST_ENTRY_BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/list_entry_background.png");
    public static final ResourceLocation LIST_ENTRY_NEW_TEXTURE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/list_entry_new.png");

    // Icons
    public static final ResourceLocation DEFAULT_ICON = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/icons/book.png");
    public static final ResourceLocation TOAST_ICON = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/book_icon.png");
    public static final ResourceLocation QUILL_ICON = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/icons/quill.png");
    public static final ResourceLocation ATTRIBUTES_SEPARATOR = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/attributes_separator.png");
    public static final ResourceLocation SCANNING_ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/scanning.png");
    public static final ResourceLocation SEASONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/seasons.png");

    // Attribute Icons
    public static final ResourceLocation HEALTH_ICON = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/attributes/health.png");
    public static final ResourceLocation ARMOR_ICON = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/attributes/armor.png");
    public static final ResourceLocation HOSTILE_ICON = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/attributes/hostile.png");
    public static final ResourceLocation PASSIVE_ICON = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/attributes/passive.png");
    public static final ResourceLocation NEUTRAL_ICON = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/attributes/neutral.png");
}
