package cluster.management;

// to keep the service registry and discovery seperated with leader election
public interface OnElectionCallback {

    // only one of those methods will be called
    void onElectedToBeLeader();

    void onWorker();
}
