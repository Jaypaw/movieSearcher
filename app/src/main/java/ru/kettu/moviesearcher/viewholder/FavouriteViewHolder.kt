package ru.kettu.moviesearcher.viewholder

import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_favourites.*
import kotlinx.android.synthetic.main.content_add_favorites.*
import kotlinx.android.synthetic.main.item_favourite.view.*
import ru.kettu.moviesearcher.activity.FavouritesActivity
import ru.kettu.moviesearcher.operations.getActivity

class FavouriteViewHolder(itemOfRecycler: View) : RecyclerView.ViewHolder(itemOfRecycler) {
    val poster: ImageView = itemOfRecycler.posterFav
    val filmName: TextView = itemOfRecycler.filmNameFav
    val deleteBtn: Button = itemOfRecycler.deleteFav

    fun bind(posterId: Int?, filmNameId: Int) {
        if (posterId == null || filmNameId == -1) return
        poster.setImageResource(posterId)
        val activity = getActivity(itemView)
        filmName.text = activity?.getString(filmNameId)
        deleteBtn.setOnClickListener {
            val activity = getActivity(itemView)
            if (activity is FavouritesActivity) {
                val element = activity.favourites.elementAt(layoutPosition)
                activity.favourites.remove(element)
                activity.notInFavourites.add(element)
                activity.recycleViewFav.adapter?.notifyItemRemoved(layoutPosition)
                activity.filmsToAddRV.adapter?.notifyItemInserted(activity.notInFavourites.indexOf(element))
            }
        }
    }
}