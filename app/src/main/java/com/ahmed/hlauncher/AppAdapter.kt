package com.ahmed.hlauncher

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Adapter for the launchable applications shown in the launcher GridView.
 *
 * App discovery and icon loading happen on a worker thread. Only the final
 * list update and view binding run on the main thread, keeping scrolling and
 * app launches responsive.
 */
class AppAdapter(
    context: Context
) : BaseAdapter() {

    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    private val inflater = LayoutInflater.from(appContext)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val loader: ExecutorService = Executors.newSingleThreadExecutor()
    private val apps = ArrayList<AppEntry>()
    private var loadGeneration = 0
    private var isClosed = false

    /**
     * Loads all activities that can be launched from the home screen.
     *
     * Call this once after assigning the adapter to the GridView. Calling it
     * again safely replaces the old list when the background work completes.
     */
    fun loadApps() {
        if (isClosed) return

        val generation = ++loadGeneration
        loader.execute {
            val loadedApps = queryLaunchableApps()

            mainHandler.post {
                if (!isClosed && generation == loadGeneration) {
                    apps.clear()
                    apps.addAll(loadedApps)
                    notifyDataSetChanged()
                }
            }
        }
    }

    override fun getCount(): Int = apps.size

    override fun getItem(position: Int): AppEntry = apps[position]

    override fun getItemId(position: Int): Long {
        // A stable id avoids unnecessary redraw work when GridView refreshes.
        return apps[position].stableId
    }

    override fun hasStableIds(): Boolean = true

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val holder: ViewHolder
        val view: View

        if (convertView == null) {
            view = inflater.inflate(R.layout.item_app, parent, false)
            holder = ViewHolder(
                icon = view.findViewById(R.id.appIcon),
                label = view.findViewById(R.id.appLabel)
            )
            view.tag = holder
            installPressAnimation(view)
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val app = getItem(position)
        holder.icon.setImageDrawable(app.icon)
        holder.label.text = app.label
        view.contentDescription = app.label

        // Use the explicit component kept during discovery, avoiding another
        // package lookup when the user taps the icon.
        view.setOnClickListener {
            launch(app)
        }

        return view
    }

    /**
     * Stops background work. Call from MainActivity.onDestroy().
     */
    fun close() {
        if (isClosed) return

        isClosed = true
        ++loadGeneration
        loader.shutdownNow()
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun queryLaunchableApps(): List<AppEntry> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos: List<ResolveInfo> =
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.MATCH_ALL
            )

        return resolveInfos
            .asSequence()
            .filter { it.activityInfo?.exported != false }
            .map { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo
                val component = ComponentName(
                    activityInfo.packageName,
                    activityInfo.name
                )
                val launchIntent = Intent(launcherIntent).apply {
                    setComponent(component)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                AppEntry(
                    label = resolveInfo.loadLabel(packageManager)
                        .toString()
                        .ifBlank { activityInfo.packageName },
                    icon = resolveInfo.loadIcon(packageManager),
                    launchIntent = launchIntent,
                    stableId = component.flattenToString().hashCode().toLong()
                )
            }
            .sortedWith(
                Comparator<AppEntry> { first, second ->
                    val labelComparison = first.label.compareTo(
                        second.label,
                        ignoreCase = true
                    )

                    if (labelComparison != 0) {
                        labelComparison
                    } else {
                        first.launchIntent.component?.packageName.orEmpty()
                            .compareTo(
                                second.launchIntent.component?.packageName.orEmpty(),
                                ignoreCase = true
                            )
                    }
                }
            )
            .toList()
    }

    private fun launch(app: AppEntry) {
        if (isClosed) return

        try {
            appContext.startActivity(app.launchIntent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                appContext,
                "Unable to open ${app.label}",
                Toast.LENGTH_SHORT
            ).show()
        } catch (_: SecurityException) {
            Toast.makeText(
                appContext,
                "This app cannot be opened",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun installPressAnimation(view: View) {
        view.setOnTouchListener { touchedView, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touchedView.animate()
                        .scaleX(0.94f)
                        .scaleY(0.94f)
                        .alpha(0.82f)
                        .setDuration(90L)
                        .start()
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    touchedView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(120L)
                        .start()
                }
            }

            // Return false so GridView still receives clicks and handles
            // pressed-state/ripple behavior normally.
            false
        }
    }

    private data class ViewHolder(
        val icon: ImageView,
        val label: TextView
    )

    data class AppEntry(
        val label: String,
        val icon: Drawable,
        val launchIntent: Intent,
        val stableId: Long
    )
}