package appeng.api.storage;


import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IUnivMonitor;
import appeng.api.util.IUnivStackIterable;


/**
 * A universal monitor callback for {@link IUnivMonitor} that receives change events for any arbitrary item type.
 */
public interface IUnivMonitorHandlerReceiver
{

    /**
     * return true if this object should remain as a listener.
     *
     * @param verificationToken to be checked object
     *
     * @return true if object should remain as a listener
     */
    boolean isValid( Object verificationToken );

    /**
     * called when changes are made to the Monitor, but only if listener is still valid.
     *
     * @param change done change
     */
    void postChange( IUnivMonitor monitor, IUnivStackIterable change, IActionSource actionSource );

    /**
     * called when the list updates its contents, this is mostly for handling power events.
     */
    void onListUpdate();
}
