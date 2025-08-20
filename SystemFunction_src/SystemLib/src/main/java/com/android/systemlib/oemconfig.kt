package com.android.systemlib

import android.annotation.SuppressLint
import android.content.Context
import android.content.RestrictionEntry
import android.content.res.TypedArray
import android.content.res.XmlResourceParser
import android.util.AttributeSet
import android.util.Xml
import com.android.internal.util.XmlUtils


private const val TAG_RESTRICTION = "restriction"
fun loadRestrictionElement(context: Context, xml: XmlResourceParser): RestrictionEntry? {
    if (xml.name == TAG_RESTRICTION) {
        try {
            val attrSet: AttributeSet = Xml.asAttributeSet(xml)
            val a: TypedArray = context.obtainStyledAttributes(
                attrSet, com.android.internal.R.styleable.RestrictionEntry
            )
            return loadRestriction(context, a, xml)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return null
}

@SuppressLint("NewApi")
private fun loadRestriction(
    context: Context, a: TypedArray, xml: XmlResourceParser
): RestrictionEntry? {
    try {
        val key: String = a.getString(com.android.internal.R.styleable.RestrictionEntry_key) ?: ""
        val restrictionType: Int =
            a.getInt(com.android.internal.R.styleable.RestrictionEntry_restrictionType, -1)
        val title = a.getString(com.android.internal.R.styleable.RestrictionEntry_title)
        val description = a.getString(com.android.internal.R.styleable.RestrictionEntry_description)
        val entries = a.getResourceId(com.android.internal.R.styleable.RestrictionEntry_entries, 0)
        val entryValues =
            a.getResourceId(com.android.internal.R.styleable.RestrictionEntry_entryValues, 0)
        val restriction = RestrictionEntry(restrictionType, key)
        restriction.title = title
        restriction.description = description

        if (entries != 0) {
//            println(entries)
            restriction.choiceEntries = context.resources.getStringArray(entries)
        }

        if (entryValues != 0) {
            restriction.choiceValues = context.resources.getStringArray(entryValues)
        }
        when (restrictionType) {
            RestrictionEntry.TYPE_STRING -> restriction.selectedString =
                a.getString(com.android.internal.R.styleable.RestrictionEntry_defaultValue)

            RestrictionEntry.TYPE_CHOICE -> restriction.selectedString =
                a.getString(com.android.internal.R.styleable.RestrictionEntry_defaultValue)

            RestrictionEntry.TYPE_INTEGER -> restriction.intValue =
                a.getInt(com.android.internal.R.styleable.RestrictionEntry_defaultValue, 0)

            RestrictionEntry.TYPE_MULTI_SELECT -> {
                val resId = a.getResourceId(
                    com.android.internal.R.styleable.RestrictionEntry_defaultValue, 0
                )
                if (resId != 0) {
                    restriction.allSelectedStrings = context.resources.getStringArray(resId)
                }
            }

            RestrictionEntry.TYPE_BOOLEAN -> restriction.selectedState = a.getBoolean(
                com.android.internal.R.styleable.RestrictionEntry_defaultValue, false
            )

            RestrictionEntry.TYPE_BUNDLE -> {
                val outerDepth = xml.depth
                val restrictionEntries: MutableList<RestrictionEntry> = ArrayList()
                while (XmlUtils.nextElementWithin(xml, outerDepth)) {
                    val childEntry = loadRestrictionElement(context, xml)
                    if (childEntry == null) {
                        println("Child entry cannot be loaded for bundle restriction $key")
                    } else {
                        restrictionEntries.add(childEntry)
                        if (restrictionType == RestrictionEntry.TYPE_BUNDLE_ARRAY && childEntry.type != RestrictionEntry.TYPE_BUNDLE) {
                            println("bundle_array $key can only contain entries of type bundle")
                        }
                    }
                }
                restriction.restrictions = restrictionEntries.toTypedArray()
            }

            RestrictionEntry.TYPE_BUNDLE_ARRAY -> {
                val outerDepth = xml.depth
                val restrictionEntries: MutableList<RestrictionEntry> = ArrayList()
                while (XmlUtils.nextElementWithin(xml, outerDepth)) {
                    val childEntry = loadRestrictionElement(context, xml)
                    if (childEntry == null) {
                        println("Child entry cannot be loaded for bundle restriction $key")
                    } else {
                        restrictionEntries.add(childEntry)
                        if (restrictionType == RestrictionEntry.TYPE_BUNDLE_ARRAY && childEntry.type != RestrictionEntry.TYPE_BUNDLE) {
                            println("bundle_array $key can only contain entries of type bundle")
                        }
                    }
                }
                restriction.restrictions = restrictionEntries.toTypedArray()
            }
        }
        return restriction
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}
