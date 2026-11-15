package id.biz.yuka.libperformx.repository

import id.biz.yuka.libperformx.SupabaseConfig
import id.biz.yuka.libperformx.entity.StoreDetailEntity
import id.biz.yuka.libperformx.entity.StoreEntity
import id.biz.yuka.libperformx.entity.toEntity
import id.biz.yuka.libperformx.request.CreateStoreRequest
import id.biz.yuka.libperformx.request.UpdateAttendanceSettingRequest
import id.biz.yuka.libperformx.request.UpdateStoreRequest
import id.biz.yuka.libperformx.response.StoreDetailResponse
import id.biz.yuka.libperformx.response.StoreResponse
import id.biz.yuka.libperformx.response.UserStoreJoinResponse
import id.biz.yuka.libperformx.utils.ResultState
import id.biz.yuka.libperformx.utils.safeSupabaseCall
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc

class StoreRepository {

    /** MENGAMBIL SEMUA TOKO (Bentuk Singkat) */
    suspend fun getAllStores(): ResultState<List<StoreEntity>> {
        return safeSupabaseCall {
            val responseList = SupabaseConfig.client.postgrest["stores"]
                .select()
                .decodeList<StoreResponse>()
            responseList.map { it.toEntity() }
        }
    }

    /** MENGAMBIL DAFTAR TOKO MILIK USER TERTENTU */
    suspend fun getMyStores(userId: String): ResultState<List<StoreEntity>> {
        return safeSupabaseCall {
            val response = SupabaseConfig.client.postgrest["user_stores"]
                .select(columns = Columns.raw("stores(*)")) {
                    filter { eq("user_id", userId) }
                }
                .decodeList<UserStoreJoinResponse>()
            
            response.map { it.stores.toEntity() }
        }
    }

    /** MENGAMBIL DETAIL TOKO + SETTING ABSEN (Dari View) */
    suspend fun getStoreDetail(storeId: String): ResultState<StoreDetailEntity> {
        return safeSupabaseCall {
            val response = SupabaseConfig.client.postgrest["vw_store_details"]
                .select { filter { eq("store_id", storeId) } }
                .decodeSingle<StoreDetailResponse>()
            response.toEntity()
        }
    }

    /** MEMBUAT TOKO BARU (Menggunakan RPC) */
    suspend fun createStore(request: CreateStoreRequest): ResultState<String> {
        return safeSupabaseCall {
            // Fungsi RPC mengembalikan UUID toko yang baru dibuat dalam bentuk String
            SupabaseConfig.client.postgrest.rpc(
                function = "create_new_store",
                parameters = request
            ).decodeAs<String>()
        }
    }

    /** MENGEDIT DATA UTAMA TOKO */
    suspend fun updateStore(storeId: String, request: UpdateStoreRequest): ResultState<Unit> {
        return safeSupabaseCall {
            SupabaseConfig.client.postgrest["stores"]
                .update(request) { filter { eq("id", storeId) } }
            Unit
        }
    }

    /** MENGEDIT PENGATURAN ABSENSI TOKO */
    suspend fun updateAttendanceSetting(storeId: String, request: UpdateAttendanceSettingRequest): ResultState<Unit> {
        return safeSupabaseCall {
            SupabaseConfig.client.postgrest["attendance_settings"]
                .update(request) { filter { eq("store_id", storeId) } }
            Unit
        }
    }

    /** MENGHAPUS TOKO (Aman dari Constraint DB) */
    suspend fun deleteStore(storeId: String): ResultState<Unit> {
        return safeSupabaseCall {
            // 1. Hapus pengaturannya dulu (karena foreign key)
            SupabaseConfig.client.postgrest["attendance_settings"]
                .delete { filter { eq("store_id", storeId) } }

            // 2. Baru hapus tokonya
            SupabaseConfig.client.postgrest["stores"]
                .delete { filter { eq("id", storeId) } }
            Unit
        }
    }
}