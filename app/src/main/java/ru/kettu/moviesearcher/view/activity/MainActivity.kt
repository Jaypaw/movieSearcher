package ru.kettu.moviesearcher.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView.OnNavigationItemSelectedListener
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import ru.kettu.moviesearcher.R
import ru.kettu.moviesearcher.controller.*
import ru.kettu.moviesearcher.models.item.FilmItem
import ru.kettu.moviesearcher.network.RetrofitApp
import ru.kettu.moviesearcher.view.fragment.FavouritesFragment
import ru.kettu.moviesearcher.view.fragment.FavouritesFragment.Companion.FAVOURITES_FRAGMENT
import ru.kettu.moviesearcher.view.fragment.FavouritesFragment.OnFavouritesFragmentAction
import ru.kettu.moviesearcher.view.fragment.FilmDetailsFragment
import ru.kettu.moviesearcher.view.fragment.FilmDetailsFragment.Companion.FILM_DETAILS_FRAGMENT
import ru.kettu.moviesearcher.view.fragment.FilmDetailsFragment.OnFilmDetailsAction
import ru.kettu.moviesearcher.view.fragment.MainFilmListFragment
import ru.kettu.moviesearcher.view.fragment.MainFilmListFragment.Companion.MAIN_FRAGMENT
import ru.kettu.moviesearcher.view.fragment.MainFilmListFragment.OnMainFragmentAction


class MainActivity : AppCompatActivity(), OnNavigationItemSelectedListener,
    OnMainFragmentAction, OnFavouritesFragmentAction, OnFilmDetailsAction {

    var filmItems = LinkedHashSet<FilmItem>()
    var selectedText: TextView? = null
    var selectedSpan: Int? = null
    var favourites = LinkedHashSet<FilmItem>()
    var toggle: ActionBarDrawerToggle? = null
    var lastAddedToFavourite: FilmItem? = null
    var currentFragmentName: String? = MAIN_FRAGMENT

    companion object {
        const val SELECTED_SPAN = "SELECTED_SPAN"
        const val FILM_INFO = "FILM_INFO"
        const val ALL_FILMS = "ALL_FILMS"
        const val FAVOURITES = "FAVOURITES"
        const val CURRENT_FRAGMENT_NAME = "CURRENT_FRAGMENT_NAME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (RetrofitApp.theMovieDbApi == null) {
            RetrofitApp.onCreate()
        }

        savedInstanceState?.let {
            selectedSpan = it.getInt(SELECTED_SPAN)
            val bundle = it.getBundle(FILM_INFO)
            favourites = bundle?.getSerializable(FAVOURITES) as LinkedHashSet<FilmItem>
            filmItems = bundle.getSerializable(ALL_FILMS) as LinkedHashSet<FilmItem>
            currentFragmentName = bundle.getString(CURRENT_FRAGMENT_NAME)
        }

        initToolbar()
        if (supportFragmentManager.findFragmentByTag(currentFragmentName) == null)
            openFragment()

        main_swipe_refresh.setOnRefreshListener {
            supportFragmentManager.refreshOpenedFragment()
            main_swipe_refresh.isRefreshing = false
        }
    }

    private fun initToolbar() {
        setSupportActionBar(mainToolbar)
        supportActionBar?.setTitle(R.string.empty)
        val toggle = ActionBarDrawerToggle(this, navigationDrawer, mainToolbar, R.string.empty, R.string.empty)
        this.toggle = toggle
        navigationDrawer.addDrawerListener(toggle)
        toggle.isDrawerIndicatorEnabled = true
        toggle.syncState()
        navigationView.setNavigationItemSelectedListener(this)
        val switch = navigationView.menu.findItem(R.id.mode)?.actionView?.findViewById<SwitchCompat>(R.id.modeSwitch)
        switch?.setOnClickListener {
            onDayNightModeSwitch(it as SwitchCompat, this)
        }
    }

    private fun openFragment() {
        when(currentFragmentName) {
            MAIN_FRAGMENT -> openMainFragment()
            FAVOURITES_FRAGMENT -> openFavouritesFragment()
        }
    }

    private fun openMainFragment() {
        currentFragmentName = MAIN_FRAGMENT
        supportFragmentManager
            .loadFragmentWithoutBackStack(R.id.fragmentContainer,
                MainFilmListFragment.newInstance(selectedSpan), MAIN_FRAGMENT)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedSpan?.let {
            outState.putInt(SELECTED_SPAN, selectedSpan!!)
        }
        val bundle = Bundle()
        bundle.putSerializable(FAVOURITES, favourites)
        bundle.putSerializable(ALL_FILMS, filmItems)
        bundle.putString(CURRENT_FRAGMENT_NAME, currentFragmentName)
        outState.putBundle(FILM_INFO, bundle)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount == 0) {
            showAlertDialog()
        } else {
            supportFragmentManager.popBackStack()
        }
    }

    override fun onAttachFragment(fragment: Fragment) {
        when (fragment) {
            is MainFilmListFragment -> {
                fragment.listener = this
            }
            is FavouritesFragment -> {
                fragment.listener = this
            }
            is FilmDetailsFragment -> {
                fragment.listener = this
            }
        }
    }

    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.mainScreen -> {
                val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                if (fragment !is MainFilmListFragment)
                    openMainFragment()
            }
            R.id.favouritesScreen -> {
                val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                if (fragment !is FavouritesFragment)
                    openFavouritesFragment()
            }
            R.id.invite -> {
                onPressInvite()
            }
            R.id.exit -> {
                showAlertDialog()
            }
        }
        navigationDrawer.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onItemsInitFinish(filmItems: Set<FilmItem>?) {
        this.filmItems = if (filmItems == null) LinkedHashSet() else filmItems as java.util.LinkedHashSet<FilmItem>
    }

    override fun onPressInvite() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        val title = resources.getString(R.string.chooser)
        val chooser = Intent.createChooser(intent, title)
        intent.resolveActivity(packageManager)?.let {
            startActivity(chooser)
        }
    }

    override fun openFavouritesFragment() {
        currentFragmentName = FAVOURITES_FRAGMENT
        supportFragmentManager
            .loadFragmentWithoutBackStack(R.id.fragmentContainer,
                FavouritesFragment.newInstance(favourites), FAVOURITES_FRAGMENT)
    }

    override fun onAddToFavourites(item: FilmItem) {
        favourites.add(item)
        lastAddedToFavourite = item
        val snackbar = Snackbar.make(fragmentContainer, R.string.addToFavourite, Snackbar.LENGTH_LONG)
            .setAction(R.string.cancel) {
                run {
                    lastAddedToFavourite?.let {
                        favourites.remove(lastAddedToFavourite as FilmItem)
                    }
                }
            }
        snackbar.view.setBackgroundColor(resources.getColor(R.color.colorMenuBase))
        snackbar.show()
    }

    override fun onDetailsBtnPressed(filmName: TextView, item: FilmItem, layoutPosition: Int) {
        filmName.setSelectedTextColor()
        if (selectedText != null)
            selectedText!!.setDefaultTextColor()

        selectedSpan = layoutPosition
        selectedText = filmName
        supportFragmentManager
            .loadFragmentWithBackStack(R.id.fragmentContainer,
                FilmDetailsFragment.newInstance(item), FILM_DETAILS_FRAGMENT)
    }

    override fun onRestoreMarkedFilmName(filmName: TextView, position: Int) {
        if (selectedSpan == position) {
            filmName.setSelectedTextColor()
            selectedText = filmName
        }
    }

    override fun onDeleteFilm(layoutPosition: Int, film: FilmItem) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (fragment is FavouritesFragment) {
            deleteFromFavourites(fragment, film, layoutPosition)
            Toast.makeText(this, R.string.deletedFromFavourite, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAddFilm(film: FilmItem) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if(fragment is FavouritesFragment) {
            addToFavourites(fragment, film)
        }
    }

    override fun onFragmentCreatedInitToolbar(fragment: Fragment) {
        when(fragment) {
            is FavouritesFragment -> main_swipe_refresh.isEnabled = false
            else -> main_swipe_refresh.isEnabled = true
        }
        updateToolbarParameters(fragment)
    }
}
