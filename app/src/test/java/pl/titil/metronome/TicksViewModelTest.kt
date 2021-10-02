package pl.titil.metronome

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.*
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class TicksViewModelTest {

    @get:Rule
    val taskRule = InstantTaskExecutorRule()

    private lateinit var lcOwner: LifecycleOwner
    private lateinit var model: TicksViewModel

    @Before
    fun setUp() {
        lcOwner = mock(LifecycleOwner::class.java).also {
            LifecycleRegistry(mock(LifecycleOwner::class.java)).apply {
                currentState = Lifecycle.State.RESUMED
                `when`(it.lifecycle).thenReturn(this)
            }
        }
        model = TicksViewModel().apply {
            setTickAt(1, TickState.SOFT)
        }
    }

    @Test
    fun testGetTickAt() {
        assertEquals(TickState.SOFT, model.getTickAt(1))
    }

    @Test
    fun testSetTickAt() {
        assumeTrue(model.getTickAt(1) == TickState.SOFT)
        model.setTickAt(1, TickState.SILENT)
        assertEquals(TickState.SILENT, model.getTickAt(1))
    }

    @Test
    fun testObserveConfig() {
        assumeTrue(model.getTickAt(1) == TickState.SOFT)
        lateinit var result: TickState
        model.config.observe(lcOwner) {
            result = it[1]
        }
        model.setTickAt(1, TickState.SILENT)
        assertEquals(TickState.SILENT, result)
    }
}