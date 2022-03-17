package com.example.ravn_challenge_v3_fabrizioflorespari.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.coroutines.await
import com.apollographql.apollo.exception.ApolloException
import com.example.ravn_challenge_v3_fabrizioflorespari.network.apolloClient
import com.example.ravn_challenge_v3_fabrizioflorespari.R
import com.example.ravn_challenge_v3_fabrizioflorespari.adapters.AllPeopleAdapter
import com.example.ravnstarwars.AllPeopleStarWarsQuery
import kotlinx.android.synthetic.main.activity_allpeople_list.*
import kotlinx.coroutines.*
import kotlin.time.ExperimentalTime


class AllPeopleActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AllPeopleActivity"
    }
    private var myApolloClient = apolloClient
    private lateinit var myAllPeopleListAdapter: AllPeopleAdapter
    private var myPeopleList = mutableListOf<AllPeopleStarWarsQuery.Person>()
    private var myCurrEndCursor: String? = null
    @ExperimentalTime
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_allpeople_list)
        myAllPeopleListAdapter = AllPeopleAdapter(this, myPeopleList)
        myAllPeopleListAdapter.onItemClickListener { person -> getDetailOf(person.id) }
        rv_all_people.adapter = myAllPeopleListAdapter
        rv_all_people.layoutManager = LinearLayoutManager(this)
        // CoroutineScope to use threads
        lifecycleScope.launch {
            while (true) {
                val result = try {
                    getDataServer()
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                    break
                }
                when (result) {
                    LoadResult.Empty -> {
                        hideLoad()
                        break
                    }
                    LoadResult.Successful -> {
                        rv_all_people.visibility = View.VISIBLE
                        runOnUiThread { ll_loading.visibility = View.VISIBLE }
                    }
                }
            }
        }
    }

    private suspend fun getDataServer(): LoadResult = coroutineScope {
        // Get list of character of the server StarWarsAPI
        val response = try {
            myApolloClient
                .query(AllPeopleStarWarsQuery(get = 5, Input.optional(myCurrEndCursor)))
                .await()
        } catch (e: ApolloException) {
            runOnUiThread { showLoadError() }
            throw e
        }
        // Check if response has any errors or is empty
        val allPeople = response.data?.allPeople
        if (allPeople == null || response.hasErrors()) {
            runOnUiThread { showLoadError() }
            throw Exception("Failed to get data from endpoint")
        }
        if (allPeople.people?.isEmpty() != false) {
            runOnUiThread { hideLoad() }
            // If it isn't more data of characters
            return@coroutineScope LoadResult.Empty
        }
        // Get another five characters and save CurrEndCursor to the next query
        val lastIndex = myPeopleList.size
        myPeopleList.addAll(allPeople.people.filterNotNull())
        runOnUiThread {
            myAllPeopleListAdapter.notifyItemRangeInserted(lastIndex, 5)
        }
        if (allPeople.pageInfo.endCursor == null)
            throw Exception("Get a null cursor")
        myCurrEndCursor = allPeople.pageInfo.endCursor
        // If is more data of characters
        return@coroutineScope LoadResult.Successful
    }

    // Shows a failed message
    private fun showLoadError() {
        ll_loading.visibility = View.GONE
        rv_all_people.visibility = View.GONE
        ll_failed.visibility = View.VISIBLE
    }

    // Hide the loading message
    private fun hideLoad(){
        ll_loading.visibility = View.GONE
    }

    private enum class LoadResult {
        Empty,
        Successful
    }

    // Extra intent that is id of clicked character
    private fun getDetailOf(id: String) {
        val extraIntent = Intent(this,  DetailledPersonsActivity::class.java)
        extraIntent.putExtra("id", id)
        startActivity(extraIntent)
    }
}