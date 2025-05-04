package com.gemahripah.banksampah.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseProvider {

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = "https://gxqnvejigdthwlkeshks.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imd4cW52ZWppZ2R0aHdsa2VzaGtzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDIwNTI5MTAsImV4cCI6MjA1NzYyODkxMH0.L3sdec1qUrZ66VS91Invpb9X60kmNvXgwKDazxklH3w"
        ) {
            install(Postgrest)
            install(Auth) {
                autoLoadFromStorage = true
            }
            install(Storage)
        }
    }
}