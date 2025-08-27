package com.gemahripah.banksampah.data.supabase

import com.gemahripah.banksampah.utils.Money2Serializer
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.math.BigDecimal

object SupabaseProvider {

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        serializersModule = SerializersModule {
            // Semua BigDecimal lewat Money3Serializer (3 desimal, HALF_UP)
            contextual(BigDecimal::class, Money2Serializer)
        }
    }

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = "https://gxqnvejigdthwlkeshks.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imd4cW52ZWppZ2R0aHdsa2VzaGtzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDIwNTI5MTAsImV4cCI6MjA1NzYyODkxMH0.L3sdec1qUrZ66VS91Invpb9X60kmNvXgwKDazxklH3w"
        ) {
            install(Postgrest) {
                // ⬅️ penting: pakai JSON di atas untuk (de)serialisasi
                serializer = KotlinXSerializer(json)
            }
            install(Auth) {
                autoLoadFromStorage = true
            }
            install(Storage)
            install(Functions)
        }
    }
}