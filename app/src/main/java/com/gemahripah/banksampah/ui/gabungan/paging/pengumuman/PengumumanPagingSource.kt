package com.gemahripah.banksampah.ui.gabungan.paging.pengumuman

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.gemahripah.banksampah.data.model.pengumuman.Pengumuman
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PengumumanPagingSource : PagingSource<Int, Pengumuman>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Pengumuman> {
        val page = params.key ?: 0
        val pageSize = params.loadSize
        val offset = page * pageSize

        return try {
            val result = withContext(Dispatchers.IO) {
                SupabaseProvider.client
                    .from("pengumuman")
                    .select(columns = Columns.list("*")) {
                        order("created_at", order = Order.DESCENDING)
                        range(offset.toLong(), (offset + pageSize - 1).toLong())
                    }
                    .decodeList<Pengumuman>()
            }

            LoadResult.Page(
                data = result,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (result.size < pageSize) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Pengumuman>): Int? =
        state.anchorPosition?.let { pos ->
            state.closestPageToPosition(pos)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(pos)?.nextKey?.minus(1)
        }
}