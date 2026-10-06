package com.benjamin.moviehub.ui.components

import androidx.paging.LoadState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class PagingLoadStateTest {
    @Test
    fun `a source refresh error with rows is a refresh error`() {
        assertTrue(isRefreshError(hasRows = true, refresh = error(), sourceRefresh = error(), mediatorRefresh = null))
    }

    @Test
    fun `a source error hidden behind a NotLoading convenience refresh is still a refresh error`() {
        // The mediator succeeded, so CombinedLoadStates.refresh reports NotLoading while the
        // source refresh actually failed; the helper must still flag it.
        assertTrue(isRefreshError(hasRows = true, refresh = idle(), sourceRefresh = error(), mediatorRefresh = idle()))
    }

    @Test
    fun `a mediator refresh error with rows is a refresh error`() {
        assertTrue(isRefreshError(hasRows = true, refresh = idle(), sourceRefresh = idle(), mediatorRefresh = error()))
    }

    @Test
    fun `a refresh error without rows is not a refresh error`() {
        assertFalse(isRefreshError(hasRows = false, refresh = error(), sourceRefresh = error(), mediatorRefresh = null))
    }

    @Test
    fun `loading or idle refreshes are not refresh errors`() {
        assertFalse(isRefreshError(hasRows = true, refresh = LoadState.Loading, sourceRefresh = LoadState.Loading, mediatorRefresh = null))
        assertFalse(isRefreshError(hasRows = true, refresh = idle(), sourceRefresh = idle(), mediatorRefresh = null))
    }

    private fun error(): LoadState = LoadState.Error(IOException("offline"))

    private fun idle(): LoadState = LoadState.NotLoading(endOfPaginationReached = false)
}
