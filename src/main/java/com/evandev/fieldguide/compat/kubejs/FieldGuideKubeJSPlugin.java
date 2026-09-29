package com.evandev.fieldguide.compat.kubejs;

//? if <1.21 {
/*import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;

public class FieldGuideKubeJSPlugin extends KubeJSPlugin {
    @Override
    public void registerEvents() {
        FieldGuideEvents.GROUP.register();
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("FieldGuide", new FieldGuideJSWrapper());
    }
}
*///?}
