package com.evandev.fieldguide.api.seasons;

import java.util.List;

public interface SeasonsProvider {
    List<Season> getGrowingSeasons(Object entry);
}
