package com.tufar.googlemapskotlin.View

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.tufar.googlemapskotlin.Model.Place
import com.tufar.googlemapskotlin.R
import com.tufar.googlemapskotlin.RoomDB.PlaceDao
import com.tufar.googlemapskotlin.RoomDB.PlaceDatabase
import com.tufar.googlemapskotlin.databinding.ActivityMapsBinding
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.lang.Exception
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationManager : LocationManager
    private lateinit var locationListener : LocationListener
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var sharedPreferences : SharedPreferences
    private lateinit var db : PlaceDatabase
    private lateinit var placeDao : PlaceDao

    private var trackBoolean : Boolean? = null
    private var selectedLatitude : Double? = null
    private var selectedLongitude : Double? = null

    val compositeDisposable = CompositeDisposable()
    var placeFromMain : Place? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        registerLauncher()

        sharedPreferences = this.getSharedPreferences("com.tufar.googlemapskotlin", MODE_PRIVATE)
        trackBoolean = false
        selectedLatitude = 0.0
        selectedLongitude = 0.0

        db = Room.databaseBuilder(applicationContext,PlaceDatabase::class.java,"Places")
            //.allowMainThreadQueries()   Bunu yazarakta database işlerinin main thread de yapılmasını sağlayabiliriz. Ama küçük veriler için.
            .build()
        placeDao = db.placeDao()

        binding.saveButton.isEnabled = false

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLongClickListener(this)

        val intent = intent
        val info = intent.getStringExtra("info")

        if (info == "new"){
            binding.saveButton.visibility = View.VISIBLE
            binding.deleteButton.visibility = View.GONE

            locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager

            locationListener = object : LocationListener{
                override fun onLocationChanged(p0: Location) {
                    // lokasyon, konum değişince yapılacak işlemler
                    //println(p0.latitude)
                    //println(p0.longitude)
                    mMap.clear()
                    trackBoolean = sharedPreferences.getBoolean("trackBoolean",false)
                    if(trackBoolean == false){
                        val guncelKonum = LatLng(p0.latitude,p0.longitude)
                        //mMap.addMarker(MarkerOptions().position(guncelKonum).title("Güncel Konumunuz"))
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(guncelKonum,15f))
                        sharedPreferences.edit().putBoolean("trackBoolean",true).apply()
                    }


                    val geocoder = Geocoder(this@MapsActivity, Locale.getDefault())

                    try {
                        val adresList = geocoder.getFromLocation(p0.latitude,p0.longitude,1)
                        if (adresList!!.size > 0 ){
                            println(adresList.get(0).toString())
                        }
                    }
                    catch (e: Exception)
                    {
                        e.printStackTrace()
                    }

                }

            }

            if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                // izin verilmemiş
                if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                    Snackbar.make(binding.root,"Permission needed for location",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission"){
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }.show()
                }else{
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            else{
                // izin verilmiş
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,locationListener)
                val sonBilinenKonum = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (sonBilinenKonum != null){
                    val sonBilinenLatLng = LatLng(sonBilinenKonum.latitude,sonBilinenKonum.longitude)
                    //mMap.addMarker(MarkerOptions().position(sonBilinenLatLng).title("Güncel Konumunuz"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sonBilinenLatLng,15f))
                }
                mMap.isMyLocationEnabled = true
            }

        }else{

            mMap.clear()
            placeFromMain = intent.getSerializableExtra("selectedPlace") as? Place
            placeFromMain?.let {
                val latlng = LatLng(it.latitude,it.longitude)
                mMap.addMarker(MarkerOptions().position(latlng).title(it.name))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng,15f))

                binding.placeText.setText(it.name)
                binding.saveButton.visibility = View.GONE
                binding.deleteButton.visibility = View.VISIBLE
            }

        }
        // ---- LatLng ----
        // Latitude -> Enlem
        // Longitude -> Boylam

        /*
        val samsun = LatLng(41.2857048, 36.3196855)
        mMap.addMarker(MarkerOptions().position(samsun).title("Marker in Kent Müzesi"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(samsun))
        */

        // casting -> as

    }

   private fun registerLauncher() {
       permissionLauncher =
           registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
               if (result) {
                   //izin verildi - permission_granted
                   if (ContextCompat.checkSelfPermission(
                           this,
                           Manifest.permission.ACCESS_FINE_LOCATION
                       ) == PackageManager.PERMISSION_GRANTED
                   ) {
                       locationManager.requestLocationUpdates(
                           LocationManager.GPS_PROVIDER,
                           0,
                           0f,
                           locationListener
                       )
                       val sonBilinenKonum =
                           locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                       if (sonBilinenKonum != null) {
                           val sonBilinenLatLng =
                               LatLng(sonBilinenKonum.latitude, sonBilinenKonum.longitude)
                           /*
                           mMap.addMarker(
                               MarkerOptions().position(sonBilinenLatLng).title("Güncel Konumunuz")
                           )

                            */
                           mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sonBilinenLatLng, 15f))
                       }
                       mMap.isMyLocationEnabled = true
                   }
                   } else {
                       // izin verilmedi - permission_denied
                       Toast.makeText(this, "Permission Needed", Toast.LENGTH_SHORT).show()
                   }
           }
   }

        override fun onMapLongClick(p0: LatLng) {

            mMap.clear()
            mMap.addMarker(MarkerOptions().position(p0))
            selectedLatitude = p0.latitude
            selectedLongitude = p0.longitude
            binding.saveButton.isEnabled = true

        }
     fun delete(view:View){

         placeFromMain?.let {
             compositeDisposable.add(
                 placeDao.delete(it)
                     .subscribeOn(Schedulers.io())
                     .observeOn(AndroidSchedulers.mainThread())
                     .subscribe(this::handleResponse))
         }
    }
     fun save(view:View){

        if (selectedLatitude != null && selectedLongitude != null){
            val place = Place(binding.placeText.text.toString(),selectedLatitude!!,selectedLongitude!!)
            compositeDisposable.add(
                placeDao.insert(place)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponse))
        }

    }
    private fun handleResponse(){
        val intent = Intent(this,MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }
}