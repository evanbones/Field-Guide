package com.evandev.fieldguide.neoforge.compat.kubejs;

//? if <26.2 {
//? if neoforge {
import com.evandev.fieldguide.compat.kubejs.FieldGuideJSWrapper;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;

public class FieldGuideKubeJSPlugin implements KubeJSPlugin {
    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(FieldGuideEvents.GROUP);
    }

    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("FieldGuide", new FieldGuideJSWrapper());
    }
}
//?}
//?}
