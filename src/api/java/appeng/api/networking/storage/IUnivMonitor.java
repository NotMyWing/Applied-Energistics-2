package appeng.api.networking.storage;


import appeng.api.storage.IUnivMonitorHandlerReceiver;


/**
 * A variant of {@link IBaseMonitor} that accepts universal listeners.
 */
public interface IUnivMonitor
{

    /**
     * add a new Listener to the monitor, be sure to properly remove yourself when you're done.
     */
    void addListener( IUnivMonitorHandlerReceiver l, Object verificationToken );

    /**
     * remove a Listener to the monitor.
     */
    void removeListener( IUnivMonitorHandlerReceiver l );
}
