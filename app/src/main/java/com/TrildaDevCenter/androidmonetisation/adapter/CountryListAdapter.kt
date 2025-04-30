package com.trildadevcenter.androidmonetisation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.trildadevcenter.androidmonetisation.databinding.AdRowLayoutBinding
import com.trildadevcenter.androidmonetisation.databinding.RowLayoutBinding
import com.trildadevcenter.androidmonetisation.model.Country
import com.trildadevcenter.androidmonetisation.model.ListItem
import com.trildadevcenter.androidmonetisation.model.TYPE_COUNTRY
import com.trildadevcenter.androidmonetisation.util.getProgressDrawable
import com.trildadevcenter.androidmonetisation.util.loadImage


class CountryListAdapter(private var countries: ArrayList<ListItem>, private val clickListener: CountryClickListener):
    RecyclerView.Adapter<CountryListAdapter.CountryListViewHolder>() {

    fun updateCountries(newCountries: ArrayList<ListItem>) {
        countries.clear()
        countries.addAll(newCountries)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) = countries[position].type

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountryListViewHolder {
        val viewHolder =
            when(viewType) {
                TYPE_COUNTRY -> {
                    val binding = RowLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                    CountryViewHolder(binding , clickListener)
                }

                else -> {
                    val binding = AdRowLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                    AdViewHolder(binding)
                }
            }

        return viewHolder
    }

    override fun getItemCount() = countries.size

    override fun onBindViewHolder(holder: CountryListViewHolder, position: Int) {
        holder.bind(countries[position])
    }

    abstract class CountryListViewHolder(binding: View): RecyclerView.ViewHolder(binding) {
        abstract fun bind(item: ListItem)
    }

    class CountryViewHolder(private val binding: RowLayoutBinding, private var clickListener: CountryClickListener): CountryListViewHolder(binding.root) {

        private val layout = binding.layout
        private val imageView = binding.imageView
        private val countryName = binding.name
        private val countryCapital = binding.capital

        override fun bind(item: ListItem) {
            val country = item as Country
            countryName.text = country.countryName
            countryCapital.text = country.capital
            imageView.loadImage(country.flag, getProgressDrawable(imageView.context))

            layout.setOnClickListener { clickListener.onCountryClick(country) }
        }
    }

    class AdViewHolder(val binding: AdRowLayoutBinding): CountryListViewHolder(binding.root) {

        private var adView = binding.adView

        override fun bind(item: ListItem) {
            val adRequest = AdRequest.Builder().build()

            adView.loadAd(adRequest)

        }
    }

    companion object {
        // This is an ad unit ID for a test ad. Replace with your own banner ad unit ID.
        private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val COUNTER_TIME = 10L
        private const val GAME_OVER_REWARD = 1
        private const val TAG = "CountryListAdapter"
    }
}