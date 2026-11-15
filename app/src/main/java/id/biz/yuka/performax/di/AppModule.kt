package id.biz.yuka.performax.di

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.biz.yuka.performax.service.FcmManager
import id.biz.yuka.performax.utils.FaceDetectionService
import id.biz.yuka.performax.utils.FaceRecognitionService
import id.biz.yuka.libperformx.utils.LocationRefreshHelper
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFaceRecognitionService(
        @ApplicationContext context: Context
    ): FaceRecognitionService {
        return FaceRecognitionService(context)
    }

    @Provides
    @Singleton
    fun provideFaceDetectionService(
        @ApplicationContext context: Context
    ): FaceDetectionService {
        return FaceDetectionService(context)
    }

    @Provides
    @Singleton
    fun provideFcmManager(): FcmManager {
        return FcmManager()
    }

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Provides
    @Singleton
    fun provideLocationRefreshHelper(): LocationRefreshHelper {
        return LocationRefreshHelper()
    }
}
