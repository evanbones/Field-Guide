package com.evandev.fieldguide.api.variant;

import java.util.List;

public record DatapackVariantDefinition(
        boolean replace,
        List<DatapackVariant> variants
) {
}