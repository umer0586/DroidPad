package com.github.umer0586.droidpad.daotests

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.github.umer0586.droidpad.data.database.AppDatabase
import com.github.umer0586.droidpad.MainDispatcherRule
import com.github.umer0586.droidpad.data.database.dao.ControlPadDao
import com.github.umer0586.droidpad.data.database.dao.ControlPadItemDao
import com.github.umer0586.droidpad.data.database.entities.ControlPad
import com.github.umer0586.droidpad.data.database.entities.ControlPadItem
import com.github.umer0586.droidpad.data.database.entities.ItemType
import com.github.umer0586.droidpad.data.database.entities.Orientation
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException


@RunWith(RobolectricTestRunner::class)
class ControlPadDaoTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var controlPadDao: ControlPadDao
    private lateinit var controlPadItemDao: ControlPadItemDao
    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()

        controlPadDao = db.controlPadDao()
        controlPadItemDao = db.controlPadItemDao()
    }

    @After
    @Throws(IOException::class)
    fun teardown() {
        db.close()
    }

    @Test
    fun `insert and retrieve control pad`() = runTest{

        val controlPadName = "test controller"
        val controlPad = ControlPad(
            name = controlPadName,
            orientation = Orientation.LANDSCAPE
        )

        val newId = controlPadDao.insert(controlPad)

        controlPadDao.getById(newId)?.let { retrievedControlPad ->
            // We cannot test the id because it is autogenerated
            assert(retrievedControlPad.name == controlPadName)
            assert(retrievedControlPad.orientation == Orientation.LANDSCAPE)
        }


    }

    @Test
    fun `delete control pad`() = runTest{

        val controlPad = ControlPad(
            name = "myController",
            orientation = Orientation.LANDSCAPE
        )

        val newId = controlPadDao.insert(controlPad)

        controlPadDao.getById(newId)?.let { retrievedControlPad ->
            controlPadDao.delete(retrievedControlPad)
        }

        assertNull(controlPadDao.getById(newId))

    }

    @Test
    fun `update control pad`() = runTest {
        val controlPadName = "myController"
        val controlPad = ControlPad(
            name = controlPadName,
            orientation = Orientation.LANDSCAPE
        )
        val newId = controlPadDao.insert(controlPad)

        controlPadDao.getById(newId)?.let { retrievedControlPad ->
            val updatedControlPad = retrievedControlPad.copy(name = "updatedController")
            controlPadDao.update(updatedControlPad)

            controlPadDao.getById(newId)?.let { updatedRetrievedControlPad ->
                assert(updatedRetrievedControlPad == updatedControlPad)
            }

        }
    }

    @Test
    fun `Test get ControlPadWithControlPadItems`() = runTest{
        var controlPad = ControlPad(
            name = "test controller",
            orientation = Orientation.LANDSCAPE
        )
        val newControlPadId = controlPadDao.insert(controlPad)

        controlPadDao.getById(newControlPadId)?.let { retrievedControlPad ->
            // controlPad with auto generated id
            controlPad = retrievedControlPad

        } ?: fail("ControlPad should not be null here")

        var controlPadItem = ControlPadItem(
            itemIdentifier = "testItem",
            controlPadId = controlPad.id,
            offsetX = 0f,
            offsetY = 0f,
            scale = 100f,
            rotation = 0f,
            itemType = ItemType.SWITCH,
            properties = "{}"
        )

        val newControlPadItemId = controlPadItemDao.insert(controlPadItem)

        controlPadItemDao.getById(newControlPadItemId)?.let { retrievedControlPadItem ->
            // controlPadItem with auto generated id
            controlPadItem = retrievedControlPadItem

        } ?: fail("ControlPadItem should not be null here")


        var controlPadItem2 = ControlPadItem(
            itemIdentifier = "testItem",
            controlPadId = controlPad.id,
            offsetX = 0f,
            offsetY = 0f,
            scale = 100f,
            rotation = 0f,
            itemType = ItemType.SWITCH,
            properties = "{}"
        )

        val newControlPadItemId2 = controlPadItemDao.insert(controlPadItem2)
        controlPadItemDao.getById(newControlPadItemId2)?.let { retrievedControlPadItem2 ->
            // controlPadItem with auto generated id
            controlPadItem2 = retrievedControlPadItem2

        } ?: fail("ControlPadItem should not be null here")

        // Map of ControlPad with List of ControlPadItems
        // Key is the ControlPad and value is the List of ControlPadItems
        val controlPadWithControlPadItems = controlPadDao.getControlPadWithControlPadItems(controlPad.id)

        // ControlPad under test should have 2 ControlPadItems
        controlPadWithControlPadItems.keys.forEach { controlPadAsKey ->

            val controlPadItems = controlPadWithControlPadItems[controlPadAsKey]

            assertEquals(controlPadAsKey, controlPad)
            assertEquals(2, controlPadItems?.size)
            assertEquals(listOf(controlPadItem, controlPadItem2), controlPadItems)
        }






    }



}