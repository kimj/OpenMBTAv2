package com.mentalmachines.ttime.data.remote

class OpenMBTAService{
    var Api: OpenWeatherMapApi

    @Inject
    fun OpenWeatherMapService(service: OpenWeatherMapApi): ??? {
        this.weatherApi = service
    }

    fun getCurrentWeather(location: String, units: String): Single<CurrentWeather> {
        return weatherApi.getCurrentWeather(location, units)
    }

}