package com.example.resqhub

/**
 * Connection settings for the Supabase project.
 *
 * The publishable key is meant to be inside the app: it can only do what the
 * database security rules (RLS) allow. Never put the "secret" or "service_role"
 * key here.
 */
object SupabaseConfig {
    const val URL = "https://mdbfrqxcweybogtpdczz.supabase.co"
    const val PUBLISHABLE_KEY = "sb_publishable_dhCoaVfYC-6B2YczZND4pg_QGXDNzqI"
}