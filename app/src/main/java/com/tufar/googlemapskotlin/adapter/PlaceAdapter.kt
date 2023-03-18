package com.tufar.googlemapskotlin.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tufar.googlemapskotlin.Model.Place
import com.tufar.googlemapskotlin.View.MapsActivity
import com.tufar.googlemapskotlin.databinding.RecyclerRowBinding

class PlaceAdapter(val placeList: List<Place>) : RecyclerView.Adapter<PlaceAdapter.Placeholder> (){
    class Placeholder(val recyclerRowBinding: RecyclerRowBinding) : RecyclerView.ViewHolder(recyclerRowBinding.root){}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Placeholder {
        val recyclerRowBinding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return Placeholder(recyclerRowBinding)
    }

    override fun onBindViewHolder(holder: Placeholder, position: Int) {
        holder.recyclerRowBinding.recyclerViewTextView.text = placeList.get(position).name
        holder.itemView.setOnClickListener{
            val intent = Intent(holder.itemView.context,MapsActivity::class.java)
            intent.putExtra("selectedPlace",placeList.get(position))
            intent.putExtra("info","old")
            holder.itemView.context.startActivity(intent)
            }
    }

    override fun getItemCount(): Int {
        return placeList.size
    }
}