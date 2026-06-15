package com.lenovo.contentprovider

import android.content.ContentUris
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lenovo.contentprovider.sqlite.DBHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 在真机/模拟器上验证 [CustomContentResolver] 对 [CustomContentProvider] 的调用。
 */
@RunWith(AndroidJUnit4::class)
class MyContentResolverInstrumentedTest {

    private lateinit var resolver: CustomContentResolver

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        resolver = CustomContentResolver(context)
        clearUsers()
    }

    @After
    fun tearDown() {
        clearUsers()
    }

    private fun clearUsers() {
        resolver.deleteUsers(null, null)
    }

    @Test
    fun insertUser_then_queryAll_returnsRow() {
        val uri = resolver.insertUser("Alice", "alice@test", 25)
        assertNotNull(uri)
        val rows = resolver.queryAllUsers()
        assertEquals(1, rows.size)
        assertEquals("Alice", rows[0].name)
        assertEquals("alice@test", rows[0].email)
        assertEquals(25, rows[0].age)
    }

    @Test
    fun queryUserById_returnsSameRow() {
        val id = ContentUris.parseId(resolver.insertUser("Bob", "bob@test", 40)!!)
        val row = resolver.queryUserByIdAsRow(id)
        assertNotNull(row)
        assertEquals(id, row!!.id)
        assertEquals("Bob", row.name)
        assertEquals(40, row.age)
    }

    @Test
    fun updateUser_changesFields() {
        val id = ContentUris.parseId(resolver.insertUser("Carol", "c@test", 20)!!)
        assertEquals(1, resolver.updateUser(id, "Carol2", "c2@test", 21))
        val row = resolver.queryUserByIdAsRow(id)!!
        assertEquals("Carol2", row.name)
        assertEquals("c2@test", row.email)
        assertEquals(21, row.age)
    }

    @Test
    fun deleteUser_removesRow() {
        val id = ContentUris.parseId(resolver.insertUser("Dan", null, null)!!)
        assertEquals(1, resolver.deleteUser(id))
        assertNull(resolver.queryUserByIdAsRow(id))
    }

    @Test
    fun deleteUsers_withSelection_deletesMatchingOnly() {
        resolver.insertUser("E1", null, 1)
        resolver.insertUser("E2", null, 2)
        val deleted = resolver.deleteUsers(
            "${DBHelper.COLUMN_USER_NAME} = ?",
            arrayOf("E1")
        )
        assertEquals(1, deleted)
        val names = resolver.queryAllUsers().map { it.name }.toSet()
        assertEquals(setOf("E2"), names)
    }

    @Test
    fun getType_returnsVndMimeForDirAndItem() {
        val dirType = resolver.getType(resolver.usersContentUri)
        assertNotNull(dirType)
        assertTrue(dirType!!.startsWith("vnd.android.cursor.dir"))

        val id = ContentUris.parseId(resolver.insertUser("Mime", null, null)!!)
        val itemType = resolver.getType(resolver.userRowUri(id))
        assertNotNull(itemType)
        assertTrue(itemType!!.startsWith("vnd.android.cursor.item"))
    }

    @Test
    fun usersContentUri_matchesCustomContentProvider() {
        assertEquals(CustomContentProvider.CONTENT_URI, resolver.usersContentUri)
    }
}
