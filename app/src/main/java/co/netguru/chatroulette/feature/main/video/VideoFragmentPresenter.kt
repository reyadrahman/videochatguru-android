package co.netguru.chatroulette.feature.main.video

import co.netguru.chatroulette.common.util.RxUtils
import co.netguru.chatroulette.data.firebase.FirebaseSignalingDisconnect
import co.netguru.chatroulette.data.firebase.FirebaseSignalingOnline
import co.netguru.chatroulette.feature.base.BasePresenter
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import timber.log.Timber
import javax.inject.Inject


class VideoFragmentPresenter @Inject constructor(
        private val firebaseSignalingOnline: FirebaseSignalingOnline,
        private val firebaseSignalingDisconnect: FirebaseSignalingDisconnect
) : BasePresenter<VideoFragmentView>() {

    private val disposables = CompositeDisposable()
    private var disconnectOrdersSubscription: Disposable = Disposables.disposed()

    override fun detachView() {
        super.detachView()
        disposables.dispose()
        disconnectOrdersSubscription.dispose()
    }

    fun startRoulette() {
        disposables += firebaseSignalingOnline.connect()
                .andThen(firebaseSignalingDisconnect.cleanDisconnectOrders())
                .doOnComplete { listenForDisconnectOrders() }
                .andThen(firebaseSignalingOnline.setOnlineAndRetrieveRandomDevice())
                .compose(RxUtils.applyMaybeIoSchedulers())
                .subscribeBy(
                        onSuccess = {
                            Timber.d("Next $it")
                            getView()?.showCamViews()
                            getView()?.connectTo(it)
                        },
                        onError = {
                            Timber.e(it, "Error while choosing random")
                            getView()?.showErrorWhileChoosingRandom()
                        },
                        onComplete = {
                            Timber.d("Done")
                            getView()?.showCamViews()
                            getView()?.showNoOneAvailable()
                        }
                )

    }

    fun listenForDisconnectOrders() {
        disconnectOrdersSubscription = firebaseSignalingDisconnect.cleanDisconnectOrders()
                .andThen(firebaseSignalingDisconnect.listenForDisconnectOrders())
                .compose(RxUtils.applyFlowableIoSchedulers())
                .subscribeBy(
                        onNext = {
                            Timber.d("Disconnect order")
                            getView()?.showOtherPartyFinished()
                            disconnect()
                        }
                )
    }

    private fun disconnect() {
        disposables += firebaseSignalingOnline.disconnect()
                .compose(RxUtils.applyCompletableIoSchedulers())
                .subscribeBy(
                        onError = {
                            Timber.d(it)
                        },
                        onComplete = {
                            disconnectOrdersSubscription.dispose()
                            getView()?.disconnect()
                            getView()?.showStartRouletteView()
                        }
                )

    }

    fun connect() {
        getView()?.attachService()
        getView()?.showLookingForPartnerMessage()
    }

    fun disconnectByUser() {
        val remoteUuid = getView()?.getRemoteUuid()
        if (remoteUuid != null) {
            disposables += firebaseSignalingDisconnect.sendDisconnectOrderToOtherParty(remoteUuid)
                    .compose(RxUtils.applyCompletableIoSchedulers())
                    .subscribeBy(
                            onComplete = {
                                disconnect()
                            }
                    )
        } else {
            disconnect()
        }

    }
}