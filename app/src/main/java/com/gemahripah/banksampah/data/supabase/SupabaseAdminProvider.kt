package com.gemahripah.banksampah.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.minimalSettings
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseAdminProvider {

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = "https://gxqnvejigdthwlkeshks.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imd4cW52ZWppZ2R0aHdsa2VzaGtzIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc0MjA1MjkxMCwiZXhwIjoyMDU3NjI4OTEwfQ.CeYzQoPostJCDJR0x5x02qXPk9QTqVVao1e-m2sHIoY"
        ) {
            install(Postgrest)
            install(Auth) {
                minimalSettings() // tidak menyimpan session user
            }
        }
    }
}