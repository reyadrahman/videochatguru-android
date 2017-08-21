package co.netguru.chatroulette.webrtc

import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import timber.log.Timber


internal class VideoPeerConnectionObserver(
        private val peerConnectionListener: PeerConnectionListener,
        private val remoteVideoListener: RemoteVideoListener) : CustomPeerConnectionObserver {

    override fun onIceCandidate(iceCandidate: IceCandidate) {
        super.onIceCandidate(iceCandidate)
        peerConnectionListener.onIceCandidate(iceCandidate)
    }

    override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState) {
        super.onIceConnectionChange(iceConnectionState)
        peerConnectionListener.onIceConnectionChange(iceConnectionState)
    }

    override fun onIceCandidatesRemoved(iceCandidates: Array<IceCandidate>) {
        super.onIceCandidatesRemoved(iceCandidates)
        peerConnectionListener.onIceCandidatesRemoved(iceCandidates)
    }

    override fun onAddStream(mediaStream: MediaStream) {
        super.onAddStream(mediaStream)
        if (mediaStream.audioTracks.size > 1 || mediaStream.videoTracks.size > 1) {
            Timber.e("Weird-looking stream: " + mediaStream)
            return
        }
        if (mediaStream.videoTracks.size == 1) {
            mediaStream.preservedVideoTracks
            val remoteVideoTrack = mediaStream.videoTracks.first
            remoteVideoListener.onAddRemoteVideoStream(remoteVideoTrack)
        }

    }

    override fun onRemoveStream(mediaStream: MediaStream) {
        super.onRemoveStream(mediaStream)
        remoteVideoListener.removeVideoStream()
    }
}