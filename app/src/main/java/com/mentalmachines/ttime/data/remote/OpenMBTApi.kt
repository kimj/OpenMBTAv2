package com.mentalmachines.ttime.data.remote

interface OpenMBTApi{
        @GET("/")
        Single<CurrentWeather> getCurrentWeather(@Query("q") String q,
        @Query("units") String units);

        @GET("/")
        Single<FiveDayThreeHour> getForecast(@Query("lat") String lat,
        @Query("lon") String lon,
        @Query("units") String units);
}