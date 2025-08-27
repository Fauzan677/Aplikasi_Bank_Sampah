package com.gemahripah.banksampah.ui.gabungan.paging.listNasabah

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class NasabahPagingSource(private val query: String) : PagingSource<Int, Pengguna>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Pengguna> {
        val page = params.key ?: 0
        val pageSize = params.loadSize
        val offset = page * pageSize

        return try {
            val response = SupabaseProvider.client
                .from("pengguna")
                .select {
                    filter {
                        eq("pgnIsAdmin", false)
                        if (query.isNotBlank()) {
                            val q = query.trim()
                            val esc = q.replace("\\", "\\\\")
                                .replace("%", "\\%")
                                .replace("_", "\\_")
                            ilike("pgnNama", "%$esc%")
                        }
                    }
                    // ⬇⬇ Urutkan alfabetis berdasarkan pgnNama
                    order("pgnNama", order = Order.ASCENDING)
                    // ⬇⬇ Lakukan pagination setelah order
                    range(offset.toLong(), (offset + pageSize - 1).toLong())
                }
                .decodeList<Pengguna>()

            val nextKey = if (response.size < pageSize) null else page + 1

            LoadResult.Page(
                data = response,
                prevKey = if (page == 0) null else page - 1,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Pengguna>): Int? {
        return state.anchorPosition?.let { position ->
            state.closestPageToPosition(position)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(position)?.nextKey?.minus(1)
        }
    }
}