/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2014 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2008 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.modules.autoupdate.ui.wizards;

import java.awt.Dialog;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.autoupdate.InstallSupport;
import org.netbeans.api.autoupdate.InstallSupport.Installer;
import org.netbeans.api.autoupdate.InstallSupport.Validator;
import org.netbeans.api.autoupdate.OperationContainer;
import org.netbeans.api.autoupdate.OperationContainer.OperationInfo;
import org.netbeans.api.autoupdate.OperationException;
import org.netbeans.api.autoupdate.OperationSupport.Restarter;
import org.netbeans.api.autoupdate.UpdateElement;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.modules.autoupdate.ui.PluginManagerUI;
import org.netbeans.modules.autoupdate.ui.ProblemPanel;
import org.netbeans.modules.autoupdate.ui.Utilities;
import org.netbeans.modules.autoupdate.ui.actions.AutoupdateCheckScheduler;
import org.netbeans.modules.autoupdate.ui.actions.AutoupdateSettings;
import static org.netbeans.modules.autoupdate.ui.wizards.Bundle.*;
import org.netbeans.modules.autoupdate.ui.wizards.LazyInstallUnitWizardIterator.LazyUnit;
import org.netbeans.modules.autoupdate.ui.wizards.OperationWizardModel.OperationType;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.WizardDescriptor;
import org.openide.awt.Mnemonics;
import org.openide.awt.Notification;
import org.openide.awt.NotificationDisplayer;
import org.openide.util.Cancellable;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Rechtacek
 */
public class InstallStep implements WizardDescriptor.FinishablePanel<WizardDescriptor> {
    private OperationPanel panel;
    private PanelBodyContainer component;
    private InstallUnitWizardModel model = null;
    private boolean clearLazyUnits = false;
    private WizardDescriptor wd = null;
    private Restarter restarter = null;
    private ProgressHandle systemHandle = null;
    private ProgressHandle spareHandle = null;
    private boolean spareHandleStarted = false;
    private boolean indeterminateProgress = false;
    private int processedUnits = 0;
    private int totalUnits = 0;
    private boolean userdirAsFallback;
    private static Notification restartNotification = null;
    private static  final Logger log = Logger.getLogger (InstallStep.class.getName ());
    private final List<ChangeListener> listeners = new ArrayList<ChangeListener> ();
    
    private static final String TEXT_PROPERTY = "text";
    
    private static final String HEAD_DOWNLOAD = "InstallStep_Header_Download_Head";
    private static final String CONTENT_DOWNLOAD = "InstallStep_Header_Download_Content";
    
    private static final String HEAD_VERIFY = "InstallStep_Header_Verify_Head";
    private static final String CONTENT_VERIFY = "InstallStep_Header_Verify_Content";
    
    private static final String HEAD_INSTALL = "InstallStep_Header_Install_Head";
    private static final String CONTENT_INSTALL = "InstallStep_Header_Install_Content";
    
    private static final String HEAD_INSTALL_DONE = "InstallStep_Header_InstallDone_Head";
    private static final String CONTENT_INSTALL_DONE = "InstallStep_Header_InstallDone_Content";
    
    private static final String HEAD_INSTALL_UNSUCCESSFUL = "InstallStep_Header_InstallUnsuccessful_Head";
    private static final String CONTENT_INSTALL_UNSUCCESSFUL = "InstallStep_Header_InstallUnsuccessful_Content";
    
    private static final String HEAD_RESTART = "InstallStep_Header_Restart_Head";
    private static final String CONTENT_RESTART = "InstallStep_Header_Restart_Content";
    
    private boolean wasStored = false;
    private boolean runInBg = false;
    private boolean canceled = false;
    private OperationException installException;
    private final boolean allowRunInBackground;
    private final boolean runInBackground;
    
    /** Creates a new instance of OperationDescriptionStep */
    public InstallStep (InstallUnitWizardModel model) {
        this (model, false);
    }
    public InstallStep (InstallUnitWizardModel model, boolean clearLazyUnits) {
        this(model, clearLazyUnits, true);
    }
    
    public InstallStep (InstallUnitWizardModel model, boolean clearLazyUnits, boolean allowRunInBackground) {
        this(model, clearLazyUnits, allowRunInBackground, false);
    }

    public InstallStep (InstallUnitWizardModel model, boolean clearLazyUnits, boolean allowRunInBackground, boolean runInBackground) {
        this.model = model;
        this.clearLazyUnits = clearLazyUnits;
        this.allowRunInBackground = allowRunInBackground;
        this.runInBackground = runInBackground;
        this.userdirAsFallback = getPreferences().getBoolean(Utilities.PLUGIN_MANAGER_DONT_CARE_WRITE_PERMISSION, false);
    }
    
    @Override
    public boolean isFinishPanel() {
        return true;
    }

    @Override
    public PanelBodyContainer getComponent() {
        if (component == null) {
            panel = new OperationPanel(allowRunInBackground, runInBackground);
            panel.addPropertyChangeListener (new PropertyChangeListener () {
                @Override
                public void propertyChange (PropertyChangeEvent evt) {
                    if (OperationPanel.RUN_ACTION.equals (evt.getPropertyName ())) {
                        try {
                            RequestProcessor.Task it = createInstallTask ();
                            PluginManagerUI.registerRunningTask (it);
                            it.waitFinished ();
                        } finally {
                            PluginManagerUI.unregisterRunningTask ();
                        }
                    } else if (OperationPanel.RUN_IN_BACKGROUND.equals (evt.getPropertyName ())) {
                        setRunInBackground ();
                    }
                }
            });
            component = new PanelBodyContainer (getBundle (HEAD_DOWNLOAD), getBundle (CONTENT_DOWNLOAD), panel);
            component.setPreferredSize (OperationWizardModel.PREFFERED_DIMENSION);
        }
        return component;
    }
    
    private RequestProcessor.Task createInstallTask () {
        return org.netbeans.modules.autoupdate.ui.actions.Installer.RP.create (new Runnable () {
            @Override
            public void run () {
                doDownloadAndVerificationAndInstall ();
            }
        });
    }
    
    @SuppressWarnings("null")
    private void doDownloadAndVerificationAndInstall () {
        
        OperationContainer<InstallSupport> installContainer = model.getBaseContainer();
        final InstallSupport support = installContainer.getSupport();
        assert support != null : "Operation failed: OperationSupport cannot be null because OperationContainer "
                + "contains elements: " + installContainer.listAll() + " and invalid elements " + installContainer.listInvalid() + "\ncontainer: " + installContainer;
        assert installContainer.listInvalid().isEmpty() : support + ".listInvalid().isEmpty() but " + installContainer.listInvalid() + " container: " + installContainer;

        if (support == null) {
            log.log(Level.WARNING, "Operation failed: OperationSupport was null because OperationContainer "
                    + "either does not contain any elements: {0} or contains invalid elements {1}",
                    new Object[]{
                        model.getBaseContainer().listAll(),
                        model.getBaseContainer().listInvalid()});
            return ;
        }
        
        Validator v;
        // download
        if ((v = handleDownload(support)) != null) {
            Installer i;
            // verifation
            if ((i = handleValidation(v, support)) != null) {
                // installation
                Restarter r;
                if ((r = handleInstall(i, support)) != null) {
                    presentInstallNeedsRestart(r, support);
                } else {
                    presentInstallDone ();
                }
            }
        }
        if (! canceled) {
            fireChange ();
        }
    }
    
    private Validator validator;
    
    private Validator handleDownload(final InstallSupport support) {
        if (canceled) {
            log.fine("Quit handleDownload() because an previous installation was canceled.");
            return null;
        }
        validator = null;
        boolean finish = false;
        while (! finish) {
            finish = tryPerformDownload(support);
        }
        
        return validator;
    }
    
    private boolean runInBackground () {
        return runInBg;
    }
    
    private void setRunInBackground () {
        if (runInBg) {
            return ;
        }
        PluginManagerUI.unregisterRunningTask ();
        runInBg = true;
        assert SwingUtilities.isEventDispatchThread () : "In AWT queue only.";
        Window w = SwingUtilities.getWindowAncestor (getComponent ());
        if (spareHandle != null && !spareHandleStarted) {
            indeterminateProgress = true;
            spareHandle.setInitialDelay(0);
            spareHandle.start();
            spareHandleStarted = true;
        }
        
        if (model.getPluginManager() != null) {
            model.getPluginManager().willClose();
        }
        if (w != null) {
            w.setVisible (false);
        }
        if (model.getPluginManager () != null) {
            model.getPluginManager ().close ();
        }
    }
    
    private boolean handleCancel () {
        if (spareHandle != null && spareHandleStarted) {
            spareHandle.finish ();
            spareHandleStarted = false;
        }
        if (systemHandle != null) {
            systemHandle.finish ();
        }
        try {
            canceled = true;
            model.doCleanup (true);
            PluginManagerUI.cancelRunningTask();
        } catch (OperationException x) {
            Logger.getLogger (InstallStep.class.getName ()).log (Level.INFO, x.getMessage (), x);
        }
        return true;
    }
    
    private boolean tryPerformDownload(final InstallSupport support) {
        validator = null;
        JLabel detailLabel = null;
        try {
            ProgressHandle handle = ProgressHandleFactory.createHandle (getBundle ("InstallStep_Download_DownloadingPlugins"));
            JComponent progressComponent = ProgressHandleFactory.createProgressComponent (handle);
            JLabel mainLabel = ProgressHandleFactory.createMainLabelComponent (handle);
            detailLabel = ProgressHandleFactory.createDetailLabelComponent (handle);
            if (runInBackground ()) {
                systemHandle = ProgressHandleFactory.createHandle (getBundle ("InstallStep_Download_DownloadingPlugins"),
                        new Cancellable () {
                            @Override
                            public boolean cancel () {
                                return handleCancel ();
                            }
                        });
                handle = systemHandle;
            } else {
                spareHandle = ProgressHandleFactory.createHandle (getBundle ("InstallStep_Download_DownloadingPlugins"),
                        new Cancellable () {
                            @Override
                            public boolean cancel () {
                                return handleCancel ();
                            }
                        });
                totalUnits = model.getBaseContainer ().listAll ().size ();
                processedUnits = 0;
                detailLabel.addPropertyChangeListener (TEXT_PROPERTY, new PropertyChangeListener () {
                    @Override
                    public void propertyChange (PropertyChangeEvent evt) {
                        assert TEXT_PROPERTY.equals (evt.getPropertyName ()) : "Listens onlo on " + TEXT_PROPERTY + " but was " + evt;
                        if (evt.getOldValue () != evt.getNewValue ()) {
                            processedUnits ++;
                            if (indeterminateProgress && spareHandleStarted) {
                                if (processedUnits < totalUnits - 1) {
                                    totalUnits = totalUnits - processedUnits;
                                    spareHandle.switchToDeterminate (totalUnits);
                                    indeterminateProgress = false;
                                }
                            }
                            if (! indeterminateProgress && spareHandleStarted) {
                                spareHandle.progress (((JLabel) evt.getSource ()).getText (), processedUnits < totalUnits - 1 ? processedUnits : totalUnits - 1);
                            }
                        }
                    }
                });
            }

            handle.setInitialDelay (0);
            panel.waitAndSetProgressComponents (mainLabel, progressComponent, detailLabel);

            validator = support.doDownload (handle, Utilities.isGlobalInstallation(), userdirAsFallback);
            if (validator == null) {
                handleCancel();
                return true;
            }
            panel.waitAndSetProgressComponents (mainLabel, progressComponent, new JLabel (getBundle ("InstallStep_Done")));
            if (spareHandle != null && spareHandleStarted) {
                spareHandle.finish ();
                spareHandleStarted = false;
            }
        } catch (OperationException ex) {
            log.log (Level.INFO, ex.getMessage (), ex);
            if (OperationException.ERROR_TYPE.PROXY == ex.getErrorType ()) {
                if (runInBackground ()) {
                    handleCancel ();
                    notifyNetworkProblem (ex);
                } else {
                    JButton tryAgain = new JButton ();
                    Mnemonics.setLocalizedText (tryAgain, getBundle ("InstallStep_NetworkProblem_Continue")); // NOI18N
                    ProblemPanel problem = new ProblemPanel (
                            getBundle ("InstallStep_NetworkProblem_Text", ex.getLocalizedMessage ()), // NOI18N
                            new JButton [] { tryAgain, model.getCancelButton (wd) });
                    Object ret = problem.showNetworkProblemDialog ();
                    if (tryAgain.equals(ret)) {
                        // try again
                        return false;
                    } else if (DialogDescriptor.CLOSED_OPTION.equals (ret)) {
                        model.getCancelButton(wd).doClick();
                    }
                }
            } else if (OperationException.ERROR_TYPE.WRITE_PERMISSION == ex.getErrorType()) {
                if (runInBackground()) {
                    UpdateElement culprit = findCulprit(ex.getMessage());
                    handleCancel();
                    notifyWritePermissionProblem(ex, culprit);
                } else {
                    JButton cancel = new JButton();
                    Mnemonics.setLocalizedText(cancel, cancel());
                    JButton install = new JButton();
                    Mnemonics.setLocalizedText(install, install());
                    UpdateElement culprit = findCulprit(ex.getMessage());
                    ProblemPanel problem = new ProblemPanel(ex, culprit, false);
                    Object ret = problem.showWriteProblemDialog();
                    if (install.equals(ret)) {
                        // install anyway
                        userdirAsFallback = true;
                        return false;
                    } else {
                        model.getCancelButton(wd).doClick();
                    }
                }
            } else {
                // general problem, show more
                String pluginName = detailLabel == null || detailLabel.getText ().length () == 0 ? getBundle ("InstallStep_DownloadProblem_SomePlugins") : detailLabel.getText ();
                String message = getBundle ("InstallStep_DownloadProblem", pluginName, ex.getLocalizedMessage ());
                Exceptions.attachLocalizedMessage (ex, message);                
                log.log (Level.SEVERE, null, ex);
                handleCancel ();
            }
        }
        return true;
        
    }
    
    private Installer handleValidation(Validator v, final InstallSupport support) {
        if (canceled) {
            log.fine("Quit handleValidation() because an previous installation was canceled.");
            return null;
        }
        component.setHeadAndContent (getBundle (HEAD_VERIFY), getBundle (CONTENT_VERIFY));
        ProgressHandle handle = ProgressHandleFactory.createHandle (getBundle ("InstallStep_Validate_ValidatingPlugins"));
        JComponent progressComponent = ProgressHandleFactory.createProgressComponent (handle);
        JLabel mainLabel = ProgressHandleFactory.createMainLabelComponent (handle);
        JLabel detailLabel = ProgressHandleFactory.createDetailLabelComponent (handle);
        if (runInBackground ()) {
            systemHandle = ProgressHandleFactory.createHandle (getBundle ("InstallStep_Validate_ValidatingPlugins"),
                        new Cancellable () {
                            @Override
                            public boolean cancel () {
                                handleCancel ();
                                return true;
                            }
                        });
            handle = systemHandle;
        } else {
            spareHandle = ProgressHandleFactory.createHandle (getBundle ("InstallStep_Validate_ValidatingPlugins"),
                    new Cancellable () {
                        @Override
                        public boolean cancel () {
                            handleCancel ();
                            return true;
                        }
                    });
            totalUnits = model.getBaseContainer ().listAll ().size ();
            processedUnits = 0;
            if (indeterminateProgress) {
                detailLabel.addPropertyChangeListener (TEXT_PROPERTY, new PropertyChangeListener () {
                    @Override
                    public void propertyChange (PropertyChangeEvent evt) {
                        assert TEXT_PROPERTY.equals (evt.getPropertyName ()) : "Listens onlo on " + TEXT_PROPERTY + " but was " + evt;
                        if (evt.getOldValue () != evt.getNewValue ()) {
                            processedUnits ++;
                            if (indeterminateProgress && spareHandleStarted) {
                                if (processedUnits < totalUnits - 1) {
                                    totalUnits = totalUnits - processedUnits;
                                    spareHandle.switchToDeterminate (totalUnits);
                                    indeterminateProgress = false;
                                }
                            }
                            if (! indeterminateProgress) {
                                spareHandle.progress (((JLabel) evt.getSource ()).getText (), processedUnits < totalUnits - 1 ? processedUnits : totalUnits - 1);
                            }
                        }
                    }
                });
            }
        }
        
        handle.setInitialDelay (0);
        panel.waitAndSetProgressComponents (mainLabel, progressComponent, detailLabel);
        if (spareHandle != null && spareHandleStarted) {
            spareHandle.finish ();
        }
        Installer tmpInst;
        
        try {
            tmpInst = support.doValidate (v, handle);
            if (tmpInst == null) return null;
        } catch (OperationException ex) {
            log.log (Level.INFO, ex.getMessage (), ex);
            ProblemPanel problem = new ProblemPanel(ex, false);
            problem.showNetworkProblemDialog ();
            handleCancel ();
            return null;
        }
        final Installer inst = tmpInst;
        List<UpdateElement> unsigned = new ArrayList<UpdateElement> ();
        List<UpdateElement> untrusted = new ArrayList<UpdateElement> ();
        String certs = "";
        for (UpdateElement el : model.getAllUpdateElements ()) {
            if (! support.isSigned (inst, el)) {
                unsigned.add (el);
            } else if (! support.isTrusted (inst, el)) {
                untrusted.add (el);
                String cert = support.getCertificate (inst, el);
                if (cert != null && cert.length () > 0) {
                    certs += getBundle ("ValidationWarningPanel_ShowCertificateFormat", el.getDisplayName (), cert);
                }
            }
        }
        if (untrusted.size () > 0 || unsigned.size () > 0 && ! runInBackground ()) {
            ValidationWarningPanel p = new ValidationWarningPanel (unsigned, untrusted);
            final JButton showCertificate = new JButton ();
            final boolean verifyCertificate = ! untrusted.isEmpty () && certs.length () > 0;
            Mnemonics.setLocalizedText (showCertificate, getBundle ("ValidationWarningPanel_ShowCertificateButton"));
            final String certificate = certs;
            showCertificate.addActionListener (new ActionListener () {
                @Override
                public void actionPerformed (ActionEvent e) {
                    if (showCertificate.equals (e.getSource ())) {
                        JTextArea ta = new JTextArea (certificate);
                        ta.setEditable (false);
                        DialogDisplayer.getDefault().notify (new NotifyDescriptor.Message (ta));
                    }
                }
            });
            final JButton canContinue = new JButton ();
            Mnemonics.setLocalizedText (canContinue, getBundle ("ValidationWarningPanel_ContinueButton"));
            final JButton cancel = model.getCancelButton (wd);
            DialogDescriptor dd = new DialogDescriptor (p, verifyCertificate ?
                getBundle ("ValidationWarningPanel_VerifyCertificate_Title") :
                getBundle ("ValidationWarningPanel_Title"));
            dd.setOptions (new JButton [] {canContinue, cancel});
            dd.setClosingOptions (new JButton [] {canContinue, cancel});
            dd.setMessageType (NotifyDescriptor.WARNING_MESSAGE);
            if (verifyCertificate) {
                dd.setAdditionalOptions (new JButton [] {showCertificate});
            }
            final Dialog dlg = DialogDisplayer.getDefault ().createDialog (dd);
            try {
                SwingUtilities.invokeAndWait (new Runnable () {
                    @Override
                    public void run () {
                        dlg.setVisible (true);
                    }
                });
            } catch (InterruptedException ex) {
                log.log (Level.INFO, ex.getLocalizedMessage (), ex);
                return null;
            } catch (InvocationTargetException ex) {
                log.log (Level.INFO, ex.getLocalizedMessage (), ex);
                return null;
            }
            if (! canContinue.equals (dd.getValue ())) {
                if (! cancel.equals (dd.getValue ())) cancel.doClick ();
                return null;
            }
            assert canContinue.equals (dd.getValue ());
        }
        panel.waitAndSetProgressComponents (mainLabel, progressComponent, new JLabel (getBundle ("InstallStep_Done")));
        return inst;
    }
    
    private Restarter handleInstall(Installer i, final InstallSupport support) {
        if (canceled) {
            log.fine("Quit handleInstall() because an previous installation was canceled.");
            return null;
        }
        installException = null;
        component.setHeadAndContent (getBundle (HEAD_INSTALL), getBundle (CONTENT_INSTALL));
        model.modifyOptionsForDisabledCancel (wd);
        
        ProgressHandle handle = ProgressHandleFactory.createHandle (getBundle ("InstallStep_Install_InstallingPlugins"));
        JComponent progressComponent = ProgressHandleFactory.createProgressComponent (handle);
        JLabel mainLabel = ProgressHandleFactory.createMainLabelComponent (handle);
        JLabel detailLabel = ProgressHandleFactory.createDetailLabelComponent (handle);
        if (runInBackground ()) {
            systemHandle = ProgressHandleFactory.createHandle (getBundle ("InstallStep_Install_InstallingPlugins"));
            handle = systemHandle;
        } else {
            spareHandle = ProgressHandleFactory.createHandle (getBundle ("InstallStep_Install_InstallingPlugins"));
            totalUnits = model.getBaseContainer ().listAll ().size ();
            processedUnits = 0;
            if (indeterminateProgress) {
                detailLabel.addPropertyChangeListener (TEXT_PROPERTY, new PropertyChangeListener () {
                    @Override
                    public void propertyChange (PropertyChangeEvent evt) {
                        assert TEXT_PROPERTY.equals (evt.getPropertyName ()) : "Listens onlo on " + TEXT_PROPERTY + " but was " + evt;
                        if (evt.getOldValue () != evt.getNewValue ()) {
                            processedUnits ++;
                            if (indeterminateProgress && spareHandleStarted) {
                                if (processedUnits < totalUnits - 1) {
                                    totalUnits = totalUnits - processedUnits;
                                    spareHandle.switchToDeterminate (totalUnits);
                                    indeterminateProgress = false;
                                }
                            }
                            if (! indeterminateProgress) {
                                spareHandle.progress (((JLabel) evt.getSource ()).getText (), processedUnits < totalUnits - 1 ? processedUnits : totalUnits - 1);
                            }
                        }
                    }
                });
            }
        }
        
        handle.setInitialDelay (0);
        panel.waitAndSetProgressComponents (mainLabel, progressComponent, detailLabel);
        Restarter r = null;
        
        boolean success = false;
        try {
            r = support.doInstall (i, handle);
            success = true;
        } catch (OperationException ex) {
            log.log (Level.INFO, ex.getMessage (), ex);
            panel.waitAndSetProgressComponents (mainLabel, progressComponent, new JLabel (
                    getBundle ("InstallStep_Unsuccessful", ex.getLocalizedMessage ())));
            installException = ex;
        }
        if (success) {
            panel.waitAndSetProgressComponents (mainLabel, progressComponent, new JLabel (getBundle ("InstallStep_Done")));
        }
        if (spareHandle != null && spareHandleStarted) {
            spareHandle.finish ();
        }
        return r;
    }
    
    private void presentInstallDone () {
        if (canceled) {
            log.fine("Quit presentInstallDone() because an previous installation was canceled.");
            return ;
        }
        model.modifyOptionsForDoClose (wd);
        if (installException == null) {
            component.setHeadAndContent (getBundle (HEAD_INSTALL_DONE), getBundle (CONTENT_INSTALL_DONE));
            panel.setBody (getBundle ("InstallStep_InstallDone_Text"),
                    model.getAllVisibleUpdateElements ());
        } else {
            component.setHeadAndContent (getBundle (HEAD_INSTALL_UNSUCCESSFUL), getBundle (CONTENT_INSTALL_UNSUCCESSFUL));
            panel.setBody (getBundle ("InstallStep_InstallUnsuccessful_Text", installException.getLocalizedMessage ()),
                    model.getAllVisibleUpdateElements ());
        }

        panel.hideRunInBackground ();
    }
    
    private void presentInstallNeedsRestart(Restarter r, final InstallSupport support) {
        if (canceled) {
            log.fine("Quit presentInstallNeedsRestart() because an previous installation was canceled.");
            return ;
        }
        component.setHeadAndContent (getBundle (HEAD_RESTART), getBundle (CONTENT_RESTART));
        model.modifyOptionsForDoClose (wd, true);
        restarter = r;
        panel.setRestartButtonsVisible (true);
        panel.setBody (getBundle ("InstallStep_InstallDone_Text"), model.getAllVisibleUpdateElements ());
        panel.hideRunInBackground ();
        if (runInBackground ()) {
            resetLastCheckWhenUpdatingFirstClassModules (model.getAllUpdateElements ());
            support.doRestartLater (restarter);
            try {
                model.doCleanup (false);
            } catch (OperationException x) {
                log.log (Level.INFO, x.getMessage (), x);
            }
            if (clearLazyUnits) {
                LazyUnit.storeLazyUnits (model.getOperation (), null);
                AutoupdateCheckScheduler.notifyAvailable (null, OperationType.UPDATE);
            }
            notifyInstallRestartNeeded (support, r); // NOI18N
        }
    }
    
    private static void notifyInstallRestartNeeded (final InstallSupport support, final Restarter r) {
        final Runnable onMouseClick = new Runnable () {
            @Override
            public void run () {
                try {
                    support.doRestart (r, null);
                } catch (OperationException x) {
                    log.log (Level.INFO, x.getMessage (), x);
                }
            }
        };
        notifyRestartNeeded (onMouseClick, getBundle ("InstallSupport_RestartNeeded"));
    }
    
    static void notifyRestartNeeded (final Runnable onMouseClick, final String tooltip) {
        //final NotifyDescriptor nd = new NotifyDescriptor.Confirmation (
        //                                    getBundle ("RestartConfirmation_Message"),
        //                                    getBundle ("RestartConfirmation_Title"),
        //                                    NotifyDescriptor.YES_NO_OPTION);
        ActionListener onClickAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //DialogDisplayer.getDefault ().notify (nd);
                //if (NotifyDescriptor.OK_OPTION.equals (nd.getValue ())) {
                    onMouseClick.run ();
                //}
            }
        };
        synchronized (InstallStep.class) {
            if (restartNotification != null) {
                restartNotification.clear();
                restartNotification = null;
            }

            restartNotification = NotificationDisplayer.getDefault().notify(tooltip,
                    ImageUtilities.loadImageIcon("org/netbeans/modules/autoupdate/ui/resources/restart.png", false),
                    getBundle("RestartNeeded_Details"), onClickAction, NotificationDisplayer.Priority.HIGH, NotificationDisplayer.Category.WARNING);
        }
    }

    private void notifyNetworkProblem (final OperationException ex) {
        // Some network problem found
        ActionListener onMouseClickAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ProblemPanel problem = new ProblemPanel(ex, false);
                problem.showNetworkProblemDialog ();
            }
        };
        String title = getBundle ("InstallSupport_InBackground_NetworkError");
        String description = getBundle ("InstallSupport_InBackground_NetworkError_Details");
        NotificationDisplayer.getDefault().notify(title, 
                ImageUtilities.loadImageIcon("org/netbeans/modules/autoupdate/ui/resources/error.png", false), 
                description, onMouseClickAction, NotificationDisplayer.Priority.HIGH, NotificationDisplayer.Category.ERROR);
    }

    @Messages({
        "# {0} - plugin_name",
        "inBackground_WritePermission=You don''t have permission to install plugin {0} into the installation directory.",
        "inBackground_WritePermission_Details=details", "cancel=Cancel", "install=Install anyway"})
    private void notifyWritePermissionProblem(final OperationException ex, final UpdateElement culprit) {
        // lack of privileges for writing
        ActionListener onMouseClickAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ProblemPanel problem = new ProblemPanel(ex, culprit, false);
                problem.showWriteProblemDialog();
            }
        };
        String title = inBackground_WritePermission(culprit.getDisplayName());
        String description = inBackground_WritePermission_Details();
        NotificationDisplayer.getDefault().notify(title,
                ImageUtilities.loadImageIcon("org/netbeans/modules/autoupdate/ui/resources/error.png", false), // NOI18N
                description, onMouseClickAction, NotificationDisplayer.Priority.HIGH, NotificationDisplayer.Category.ERROR);
    }

    @Override
    public HelpCtx getHelp() {
        return null;
    }

    @Override
    public void readSettings (WizardDescriptor wd) {
        this.wd = wd;
        this.wasStored = false;
    }

    @Override
    public void storeSettings (WizardDescriptor wd) {
        assert ! WizardDescriptor.PREVIOUS_OPTION.equals (wd.getValue ()) : "Cannot invoke Back in this case.";
        if (wasStored) {
            return ;
        }
        this.wasStored = true;
        InstallSupport support = model.getBaseContainer().getSupport();
        if (WizardDescriptor.CANCEL_OPTION.equals (wd.getValue ()) || WizardDescriptor.CLOSED_OPTION.equals (wd.getValue ())) {
            try {
                model.doCleanup (true);
            } catch (OperationException x) {
                Logger.getLogger (InstallStep.class.getName ()).log (Level.INFO, x.getMessage (), x);
            }
        } else if (restarter != null) {
            if (support == null) {
                assert model.getBaseContainer().listAll() == null : "storeSettings failed. OperationSupport is null because OperationContainer " +
                        "contains no elements: " + model.getBaseContainer ().listAll ();
                return ;
            }
            if (panel.restartNow ()) {
                resetLastCheckWhenUpdatingFirstClassModules (model.getAllUpdateElements ());
                handleLazyUnits (clearLazyUnits, false);
                try {
                    support.doRestart (restarter, null);
                } catch (OperationException x) {
                    log.log (Level.INFO, x.getMessage (), x);
                }
                
            } else {
                resetLastCheckWhenUpdatingFirstClassModules (model.getAllUpdateElements ());
                support.doRestartLater (restarter);
                handleLazyUnits (clearLazyUnits, true);
                try {
                    model.doCleanup (false);
                } catch (OperationException x) {
                    log.log (Level.INFO, x.getMessage (), x);
                }
                notifyInstallRestartNeeded (support, restarter); // NOI18N
            }
        } else {
            try {
                model.doCleanup (! WizardDescriptor.FINISH_OPTION.equals (wd.getValue ()));
            } catch (OperationException x) {
                log.log (Level.INFO, x.getMessage (), x);
            }
        }
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public synchronized void addChangeListener(ChangeListener l) {
        listeners.add(l);
    }

    @Override
    public synchronized void removeChangeListener(ChangeListener l) {
        listeners.remove(l);
    }
    
    private void handleLazyUnits (boolean clearLazyUnits, boolean notifyUsers) {
        if (clearLazyUnits) {
            LazyUnit.storeLazyUnits (model.getOperation (), null);
            if (notifyUsers) {
                AutoupdateCheckScheduler.notifyAvailable (null, OperationType.UPDATE);
            }
        } else {
            // get LazyUnit being installed
            Collection<String> tmp = new HashSet<String> ();
            for (UpdateElement el : model.getAllUpdateElements ()) {
                tmp.add (LazyUnit.toString (el));
            }
            // remove them from LazyUnits stored for next IDE run
            Collection<LazyUnit> res = new HashSet<LazyUnit> ();
            for (LazyUnit lu : LazyUnit.loadLazyUnits (model.getOperation ())) {
                if (! tmp.contains (lu.toString ())) {
                    res.add (lu);
                }
            }
            LazyUnit.storeLazyUnits (model.getOperation (), res);
            if (notifyUsers) {
                AutoupdateCheckScheduler.notifyAvailable (res, OperationType.UPDATE);
            }
        }
    }

    private void fireChange() {
        ChangeEvent e = new ChangeEvent(this);
        List<ChangeListener> templist;
        synchronized (this) {
            templist = new ArrayList<ChangeListener> (listeners);
        }
	for (ChangeListener l: templist) {
            l.stateChanged(e);
        }
    }

    private static String getBundle (String key, Object... params) {
        return NbBundle.getMessage (InstallStep.class, key, params);
    }
    
    private static void resetLastCheckWhenUpdatingFirstClassModules (Collection<UpdateElement> toUpdate) {
        boolean resetChecking = false;
        for (UpdateElement el : toUpdate) {
            if (Utilities.getFirstClassModules ().contains (el.getCodeName ())) {
                resetChecking = true;
                break;
            }
        }
        if (resetChecking) {
            AutoupdateSettings.setLastCheck (null);
        }
    }
    
    private static Preferences getPreferences() {
        return NbPreferences.forModule(Utilities.class);
    }

    private UpdateElement findCulprit(String codeName) {
        if (codeName == null || codeName.isEmpty()) {
            return null;
        }
        for (OperationInfo<InstallSupport> info : model.getBaseContainer().listAll()) {
            if (codeName.equals(info.getUpdateElement().getCodeName())) {
                return info.getUpdateElement();
            }
        }
        return null;
    }
    
}
