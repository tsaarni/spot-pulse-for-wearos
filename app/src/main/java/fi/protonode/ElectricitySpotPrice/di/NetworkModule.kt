package fi.protonode.ElectricitySpotPrice.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fi.protonode.ElectricitySpotPrice.network.SpotHintaApi
import javax.inject.Singleton
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    // This Hilt module tells Hilt how to build and share single instances
    // of networking dependencies across the app. @InstallIn(SingletonComponent::class)
    // means these objects live as long as the app process (singletons).

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
                .baseUrl("https://api.spot-hinta.fi/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }

    @Provides
    @Singleton
    fun provideSpotHintaApi(retrofit: Retrofit): SpotHintaApi {
        return retrofit.create(SpotHintaApi::class.java)
    }
}
