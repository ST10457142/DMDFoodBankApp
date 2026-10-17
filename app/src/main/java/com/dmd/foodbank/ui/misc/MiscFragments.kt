package com.dmd.foodbank.ui.misc

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.dmd.foodbank.data.model.Hub
import com.dmd.foodbank.data.model.StockStatus
import com.dmd.foodbank.databinding.*
import com.dmd.foodbank.ui.ContactViewModel
import com.dmd.foodbank.ui.DonateViewModel
import com.google.android.material.snackbar.Snackbar
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.common.MapboxOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.dmd.foodbank.R

// ─── About ────────────────────────────────────────────────────────────────────

class AboutFragment : Fragment() {
    private var _b: FragmentAboutBinding? = null
    private val b get() = _b!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentAboutBinding.inflate(i, c, false).also { _b = it }.root

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

// ─── Locations ────────────────────────────────────────────────────────────────

class LocationsFragment : Fragment() {
    private var _b: FragmentLocationsBinding? = null
    private val b get() = _b!!
    private val vm: DonateViewModel by viewModels()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        run {
            MapboxOptions.accessToken = getString(R.string.mapbox_access_token)
            FragmentLocationsBinding.inflate(i, c, false).also { _b = it }.root
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        b.rvHubs.layoutManager = LinearLayoutManager(requireContext())
        val hubs = vm.hubs()
        b.rvHubs.adapter = HubsAdapter(hubs)
        setupMap(hubs)
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private fun setupMap(hubs: List<Hub>) {
        // Let the map pan/zoom without the surrounding scroll view stealing the gesture
        b.mapView.setOnTouchListener { v, _ ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }
        b.mapView.mapboxMap.setCamera(
            CameraOptions.Builder().center(Point.fromLngLat(28.0, -26.2)).zoom(8.5).build()
        )
        b.mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) {
            val manager = b.mapView.annotations.createCircleAnnotationManager()
            manager.create(hubs.map { hub ->
                CircleAnnotationOptions()
                    .withPoint(Point.fromLngLat(hub.longitude, hub.latitude))
                    .withCircleRadius(9.0)
                    .withCircleColor(
                        when (hub.stockStatus) {
                            StockStatus.FULL -> "#2E7D32"
                            StockStatus.LOW -> "#F9A825"
                            StockStatus.CRITICAL -> "#C62828"
                        }
                    )
                    .withCircleStrokeColor("#FFFFFF")
                    .withCircleStrokeWidth(2.0)
            })
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

class HubsAdapter(private val hubs: List<Hub>) :
    androidx.recyclerview.widget.RecyclerView.Adapter<HubsAdapter.VH>() {

    inner class VH(val b: ItemHubBinding) : androidx.recyclerview.widget.RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, v: Int) =
        VH(ItemHubBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun getItemCount() = hubs.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val hub = hubs[pos]
        h.b.tvHubName.text = hub.name
        h.b.tvHubAddress.text = hub.address
        h.b.tvHubHours.text = "🕐 ${hub.hours}"
        h.b.tvHubPhone.text = "📞 ${hub.phone}"
        h.b.tvHubStock.text = "${hub.stockStatus.emoji} ${hub.stockStatus.label}"
        h.b.tvHubStock.setTextColor(
            when (hub.stockStatus) {
                StockStatus.FULL -> h.b.root.context.getColor(android.R.color.holo_green_dark)
                StockStatus.LOW -> h.b.root.context.getColor(android.R.color.holo_orange_dark)
                StockStatus.CRITICAL -> h.b.root.context.getColor(android.R.color.holo_red_dark)
            }
        )
        h.b.btnDonateHere.setOnClickListener {
            Toast.makeText(h.b.root.context, "Navigate to Donate screen to select ${hub.name}", Toast.LENGTH_SHORT).show()
        }
    }
}

// ─── Contact ──────────────────────────────────────────────────────────────────

class ContactFragment : Fragment() {
    private var _b: FragmentContactBinding? = null
    private val b get() = _b!!
    private val vm: ContactViewModel by viewModels()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentContactBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        b.btnSend.setOnClickListener {
            vm.sendMessage(
                b.etName.text.toString(),
                b.etEmail.text.toString(),
                b.etSubject.text.toString(),
                b.etMessage.text.toString()
            )
        }

        vm.loading.observe(viewLifecycleOwner) { b.progressBar.visibility = if (it) View.VISIBLE else View.GONE }

        vm.error.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrEmpty()) { Snackbar.make(b.root, msg, Snackbar.LENGTH_LONG).show(); vm.clearMessages() }
        }

        vm.success.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrEmpty()) {
                Snackbar.make(b.root, msg, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(requireContext().getColor(android.R.color.holo_green_dark)).show()
                b.etName.text?.clear(); b.etEmail.text?.clear()
                b.etSubject.text?.clear(); b.etMessage.text?.clear()
                vm.clearMessages()
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
