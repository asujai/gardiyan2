package com.gardiyan.app

import com.gardiyan.app.data.model.DayStatus
import com.gardiyan.app.ui.components.ChainLink
import com.gardiyan.app.ui.components.DisciplineChain
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Disiplin ızgarasındaki zincir kuralları.
 *
 * Amaç: ardışık başarılı günler görsel olarak bağlansın, ihlal edilen gün
 * zinciri görünür biçimde koparsın.
 */
class DisciplineChainTest {

    @Test
    fun `two successful days are forged together`() {
        assertEquals(
            ChainLink.FORGED,
            DisciplineChain.link(DayStatus.SUCCESS, DayStatus.SUCCESS)
        )
    }

    @Test
    fun `failure breaks the chain on both sides`() {
        assertEquals(
            ChainLink.NONE,
            DisciplineChain.link(DayStatus.SUCCESS, DayStatus.FAILURE)
        )
        assertEquals(
            ChainLink.NONE,
            DisciplineChain.link(DayStatus.FAILURE, DayStatus.SUCCESS)
        )
    }

    @Test
    fun `empty day breaks the chain`() {
        assertEquals(
            ChainLink.NONE,
            DisciplineChain.link(DayStatus.SUCCESS, DayStatus.NONE)
        )
        assertEquals(
            ChainLink.NONE,
            DisciplineChain.link(DayStatus.NONE, DayStatus.SUCCESS)
        )
    }

    @Test
    fun `ongoing day keeps the chain alive with a distinct link`() {
        assertEquals(
            ChainLink.LIVE,
            DisciplineChain.link(DayStatus.SUCCESS, DayStatus.PROGRESS)
        )
        assertEquals(
            ChainLink.LIVE,
            DisciplineChain.link(DayStatus.PROGRESS, DayStatus.SUCCESS)
        )
    }

    @Test
    fun `longest run counts the unbroken stretch`() {
        val row = listOf(
            DayStatus.SUCCESS,
            DayStatus.FAILURE,
            DayStatus.SUCCESS,
            DayStatus.SUCCESS,
            DayStatus.SUCCESS,
            DayStatus.NONE,
            DayStatus.SUCCESS
        )
        assertEquals(3, DisciplineChain.longestRunInRow(row))
    }

    @Test
    fun `longest run is zero when nothing is chainable`() {
        val row = listOf(DayStatus.FAILURE, DayStatus.NONE, DayStatus.FAILURE)
        assertEquals(0, DisciplineChain.longestRunInRow(row))
    }

    @Test
    fun `single successful day counts as one`() {
        val row = listOf(DayStatus.NONE, DayStatus.SUCCESS, DayStatus.NONE)
        assertEquals(1, DisciplineChain.longestRunInRow(row))
    }
}
