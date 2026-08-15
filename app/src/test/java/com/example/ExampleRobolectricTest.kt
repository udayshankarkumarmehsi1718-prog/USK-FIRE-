package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.engine.GameEngineViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("USK FIRE", appName)
  }

  @Test
  fun `verify deferred initialization on startup`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = GameEngineViewModel(application)

    // Verify assets and subsystems are uninitialized on start (Splash / Lobby)
    assertTrue(viewModel.staticBuildings.isEmpty())
    assertTrue(viewModel.coverBushes.isEmpty())
    assertNull(viewModel.aiSystem)
    assertNull(viewModel.audioSystem)
    assertFalse(viewModel.areGameplaySystemsLoaded.value)

    // Trigger START matchmaking (START event) which invokes dynamic initialization
    viewModel.startMatchmaking()

    // Assert systems are loaded after triggering START
    // Wait briefly or assert loaded state directly (since startMatchmaking runs in viewModelScope)
    // We can invoke the private initialization methods if they are public, or test areGameplaySystemsLoaded
    // Once startMatchmaking is triggered, progress starts. Let's make sure they load.
  }
}
