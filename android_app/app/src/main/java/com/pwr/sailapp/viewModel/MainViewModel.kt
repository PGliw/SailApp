package com.pwr.sailapp.viewModel

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.*
import com.pwr.sailapp.data.network.TOKEN_EXPIRED
import com.pwr.sailapp.data.network.sail.ConnectivityInterceptorImpl
import com.pwr.sailapp.data.network.sail.SailAppApiService
import com.pwr.sailapp.data.network.sail.response.CHANGE_SUCCESS_MSG
import com.pwr.sailapp.data.network.sail.response.CancelResponse
import com.pwr.sailapp.data.network.sail.response.RENTAL_OK_MESSAGE
import com.pwr.sailapp.data.network.weather.DarkSkyApiService
import com.pwr.sailapp.data.sail.*
import com.pwr.sailapp.internal.ErrorCodeException
import com.pwr.sailapp.internal.NetworkStatus
import com.pwr.sailapp.internal.NoConnectivityException
import com.pwr.sailapp.utils.DateUtil
import com.pwr.sailapp.utils.FiltersAndLocationUtil.calculateDistances
import com.pwr.sailapp.utils.FiltersAndLocationUtil.filterAndSortCentres
import com.pwr.sailapp.viewModel.TokenHandler.NO_TOKEN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

const val NO_DATE = ""

/*
https://developer.android.com/guide/navigation/navigation-conditional#kotlin
https://developer.android.com/reference/android/arch/lifecycle/AndroidViewModel - AndroidViewModel provides application (and its context!)
 */
class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    companion object {
        const val INITIAL_MIN_RATING = 0.0
        const val INITIAL_MAX_DISTANCE = 1000000.00
        const val INITIAL_RENT_AMOUNT_LIMIT = 10
        const val INITIAL_START_TIME_SHIFT = 1
        const val INITIAL_END_TIME_SHIFT = 2
    }

    private val appContext = application.applicationContext

    private val sailAppApiService = SailAppApiService(connectivityInterceptor = ConnectivityInterceptorImpl(appContext))
    private val darkSkyApiService = DarkSkyApiService(connectivityInterceptor = ConnectivityInterceptorImpl(appContext))

    var rentalToCancel: Rental? = null

    val user = MutableLiveData<User>()
    val upcomingRentals = MutableLiveData<List<Rental>>()
    val allRentals = MutableLiveData<List<Rental>>()
    private val allCentres = MutableLiveData<List<Centre>>()
    val networkStatus = MutableLiveData<NetworkStatus>()
    val cancelStatus = MutableLiveData<CancelRentalStatus>()

    val centres = MediatorLiveData<List<Centre>>().apply {
        addSource(allCentres) {
            this.value = it
        }
    }

    var location: Location? = null

    lateinit var selectedCentre: Centre
    //  var selectedCentreId = -1

    var gearList = ArrayList<Gear>()

    var selectedGearId = -1
    var rentAmountLimit = INITIAL_RENT_AMOUNT_LIMIT
    var rentAmount = 1
    // var rentStart = "2019-05-30T08:25:31.129Z"
    // var rentEnd = "2019-05-30T08:35:31.129Z"

    var rentStart: Date? = null
        get() {
            if (field == null) {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.HOUR_OF_DAY, INITIAL_START_TIME_SHIFT)
                calendar[Calendar.MINUTE] = 0
                calendar[Calendar.SECOND] = 0
                field = calendar.time
            }
            return field
        }

    var rentEnd: Date? = null
        get() {
            if (field == null) {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.HOUR_OF_DAY, INITIAL_END_TIME_SHIFT)
                calendar[Calendar.MINUTE] = 0
                calendar[Calendar.SECOND] = 0
                field = calendar.time
            }
            return field
        }

    var rentID = -1

    val authenticationState = MutableLiveData<AuthenticationState>()
    val rentalState = MutableLiveData<RentalState>()
    val changeDataState = MutableLiveData<ChangeDataState>()

    var isCancellationAllowed: Boolean = false

    // Dialogs
    var minRating = INITIAL_MIN_RATING
    var maxDistance = INITIAL_MAX_DISTANCE
    var actualDistance = INITIAL_MAX_DISTANCE
    var isAscendingSort = true

    init {
        if (TokenHandler.refreshToken == NO_TOKEN) {
            authenticationState.value = AuthenticationState.UNAUTHENTICATED
        }
    }


    private suspend fun doNetworkOperation(
        logic: suspend () -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                logic()
                networkStatus.postValue(NetworkStatus.CONNECTED)
            } catch (e: NoConnectivityException) {
                networkStatus.postValue(NetworkStatus.DISCONNECTED)
                Log.e("doNetworkOperation", "No connectivity exception")

            } catch (e: ErrorCodeException) {
                when (e.code) {
                    401 -> if (e.message == TOKEN_EXPIRED) {
                        runBlocking(Dispatchers.IO) {
                            refreshAuthToken()
                        }
                    } else authenticationState.postValue(AuthenticationState.UNAUTHENTICATED)// 401 UNAUTHORIZED
                    403 -> cancelStatus.postValue(CancelRentalStatus.CANCELLATION_FAILED)
                }
                Log.e("doNetworkOperation", "${e.code} ${e.message}")
            }
        }
    }

    suspend fun fetchUser() = doNetworkOperation {
        val userDeferred = sailAppApiService.getUserDataAsync("Bearer ${TokenHandler.accessToken}")
        val userRes = userDeferred.await()
        user.postValue(userRes)
    }

    suspend fun fetchUpcomingRentals() = doNetworkOperation {
        val rentalsDeferred = sailAppApiService.getAllUserRentals("Bearer ${TokenHandler.accessToken}")
        val rentalsRes = rentalsDeferred.await()
        //
        val futureRentals = rentalsRes.filter {
            if(it.rentStartDate == null){Log.e("fetchRentals", "Filter: rentStartDate = null"); false}
            else DateUtil.isFutureDate(it.rentStartDate!!)
        }
        for (rental in futureRentals) {
            if (DateUtil.isForecastAvailable(rental.rentStartDate)) {
                val forecastDeferred = darkSkyApiService.getForecastAsync(
                    rental.centreLatitude,
                    rental.centreLongitude,
                    rental.timestampSecs!!
                )
                val forecastRes = forecastDeferred.await()
                rental.currently = forecastRes.currently
            }
        }

        val sortedFutureRentals = futureRentals.sortedBy {
            it.timestampSecs
        }

        upcomingRentals.postValue(sortedFutureRentals)
    }

    suspend fun fetchAllRentals() = doNetworkOperation {
        val rentalsDeferred = sailAppApiService.getAllUserRentals("Bearer ${TokenHandler.accessToken}")
        val rentalsRes = rentalsDeferred.await()
        allRentals.postValue(rentalsRes)
    }

    private suspend fun refreshAuthToken() = doNetworkOperation {
        val tokenDeferred = sailAppApiService.refreshAuthTokenAsync("Bearer ${TokenHandler.refreshToken}")
        val tokenRes = tokenDeferred.await()
        TokenHandler.accessToken = tokenRes
    }

    suspend fun fetchCentres() = doNetworkOperation {
        val centresDeferred = sailAppApiService.getCentresAsync("Bearer ${TokenHandler.accessToken}")
        val centresRes = centresDeferred.await()
        for(centre in centresRes){
            val picDeferred = sailAppApiService.getPicturesOfCentreAsync("Bearer ${TokenHandler.accessToken}", centre.ID)
            val picRes = picDeferred.await()
            if(picRes.isNotEmpty()){
                if(picRes[0].picture_id != "1"){
                    centre.photoURL = picRes[0].picture_link
                }
            }
        }
        allCentres.postValue(centresRes)
    }

    suspend fun fetchGear() = doNetworkOperation {
        val gearDeferred = sailAppApiService.getAllGearAsync("Bearer ${TokenHandler.accessToken}", selectedCentre.ID)
        val gearRes = gearDeferred.await()
        gearList = ArrayList(gearRes)
    }

    suspend fun rentGear() = doNetworkOperation {
        if (!areDatesCorrect(rentStart!!, rentEnd!!)){
            rentalState.postValue(RentalState.RENTAL_FAILED)
            return@doNetworkOperation
        }
        val rentStartStr = DateUtil.dateToString(rentStart)
        val rentEndStr = DateUtil.dateToString(rentEnd)

        val rentDeferred =
            sailAppApiService.rentGearAsync(
                "Bearer ${TokenHandler.accessToken}",
                centreID = selectedCentre.ID,
                gearID = selectedGearId,
                rentAmount = rentAmount,
                rentStart = rentStartStr,
                rentEnd = rentEndStr
            )
        val rentRes = rentDeferred.await()
        when (rentRes.msg) {
            RENTAL_OK_MESSAGE -> rentalState.postValue(RentalState.RENTAL_SUCCESSFUL)
            else -> rentalState.postValue(RentalState.RENTAL_FAILED)
        }
    }

    private fun areDatesCorrect(startDate: Date, endDate: Date):Boolean{
        val currentDate = Calendar.getInstance().time
        return (startDate >= currentDate && endDate > startDate)
    }

    suspend fun cancelRental(rentID: Int) = doNetworkOperation {
        if (isCancellationAllowed) {
            val cancelDeferred =
                sailAppApiService.cancelRentAsync(
                    "Bearer ${TokenHandler.accessToken}",
                    rentID = rentID
                )
            val cancelRes = cancelDeferred.await()
            when(cancelRes.msg){
                CancelResponse.CANCEL_OK_MSG -> cancelStatus.postValue(CancelRentalStatus.CANCELLATION_SUCCESS)
                else -> cancelStatus.postValue(CancelRentalStatus.CANCELLATION_FAILED)
            }
            isCancellationAllowed = false
            rentalToCancel = null
        }
    }

    fun logOut() {
        TokenHandler.accessToken = NO_TOKEN
        TokenHandler.refreshToken = NO_TOKEN
        authenticationState.value = AuthenticationState.UNAUTHENTICATED
    }

    // Search (filter) centres by name
    fun search(query: String?) {
        if (query == null) {
            Log.e("MainViewModel", "search: query = null"); return
        }
        if (centres.value == null) {
            Log.e("MainViewModel", "search: centres.value = null"); return
        }
        val queryLowerCase = query.toLowerCase(Locale.getDefault())
        if (queryLowerCase.isNotEmpty()) {
            val filteredCentres = centres.value!!.filter { centre: Centre ->
                centre.name.toLowerCase(Locale.getDefault()).contains(queryLowerCase) // && centre.rating>minRating
            }
            centres.value = ArrayList(filteredCentres)
        } else centres.value = filterAndSortCentres(allCentres.value, isAscendingSort, actualDistance)
    }


    fun applyFilter() {
        if (allCentres.value == null) {
            Log.e("MainViewModel", "applyFilter: allCentres.value = null"); return
        }
        centres.value = filterAndSortCentres(allCentres.value, isAscendingSort, actualDistance)
    }

    fun applySort() {
        if (centres.value == null) {
            Log.e("MainViewModel", "applySort: allCentres.value = null"); return
        }
        centres.value = filterAndSortCentres(allCentres.value, isAscendingSort, actualDistance)
    }

    fun applyLocation() {
        if (location == null) {
            Log.e("MainViewModel", "calculateDistances: location = null"); return
        }
        centres.value = calculateDistances(centres.value, location)
    }

    fun isChangedDataValid(
        firstName: String,
        lastName: String,
        phoneNumber: String,
        email: String
    ): Boolean {
        return firstName.isNotEmpty() &&
                lastName.isNotEmpty() &&
                phoneNumber.isNotEmpty() &&
                email.isNotEmpty()
    }

    suspend fun changeUserData(
        firstName : String,
        lastName: String,
        email: String,
        phoneNumber: String
    ) = doNetworkOperation{
        val changeDataDeferred =
            sailAppApiService.changeDataAsync(
                "Bearer ${TokenHandler.accessToken}",
                firstName, lastName, email, phoneNumber
            )
        val changeDataRes = changeDataDeferred.await()
        when (changeDataRes.msg) {
            CHANGE_SUCCESS_MSG -> changeDataState.postValue(ChangeDataState.CHANGE_DATA_SUCCESSFUL)
            else -> changeDataState.postValue(ChangeDataState.CHANGE_DATA_FAILED)
        }
    }
}

