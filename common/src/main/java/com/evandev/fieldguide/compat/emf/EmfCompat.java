package com.evandev.fieldguide.compat.emf;

import com.evandev.fieldguide.Constants;
import traben.entity_model_features.models.animation.state.EMFState;

public class EmfCompat {
    private static boolean warned = false;

    public static void setInGui(boolean inGui) {
        try {
            EMFState.isInGui = inGui;
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                Constants.LOG.warn("Could not set EMF's in-GUI flag; Field Guide entity models may look incorrect.", t);
            }
        }
    }
}
