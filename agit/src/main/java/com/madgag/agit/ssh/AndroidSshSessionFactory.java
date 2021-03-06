/*
 * Copyright (c) 2011, 2012 Roberto Tyley
 *
 * This file is part of 'Agit' - an Android Git client.
 *
 * Agit is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Agit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/ .
 */

package com.madgag.agit.ssh;

import android.os.RemoteException;
import android.util.Log;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import com.madgag.ssh.android.authagent.AndroidAuthAgent;
import com.madgag.ssh.authagent.client.jsch.SSHAgentIdentity;

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.util.FS;

public class AndroidSshSessionFactory extends JschConfigSessionFactory {

    private static final String TAG = "ASSF";

    private final Provider<AndroidAuthAgent> androidAuthAgentProvider;
    private final UserInfo userInfo;
    private final HostKeyRepository hostKeyRepository;

    @Inject
    public AndroidSshSessionFactory(Provider<AndroidAuthAgent> androidAuthAgentProvider, UserInfo userInfo,
                                    HostKeyRepository hostKeyRepository) {
        this.androidAuthAgentProvider = androidAuthAgentProvider;
        this.userInfo = userInfo;
        this.hostKeyRepository = hostKeyRepository;
    }

    @Override
    protected void configure(OpenSshConfig.Host host, Session session) {
        session.setConfig("StrictHostKeyChecking", "yes"); // let the hostKeyRepository ask the questions
        session.setUserInfo(userInfo);
    }

    @Override
    protected JSch createDefaultJSch(FS fs) throws JSchException {
        final JSch jsch = new JSch();
        jsch.setHostKeyRepository(hostKeyRepository);
        addSshAgentTo(jsch);
        return jsch;
    }

    private void addSshAgentTo(final JSch jsch) throws JSchException {
        AndroidAuthAgent authAgent = androidAuthAgentProvider.get();
        Log.w(TAG, "authAgent=" + authAgent);
        if (authAgent == null) {
            Log.w(TAG, "NO SSH-AGENT AVAILABLE");
        } else {
            updateJschWithAvailableIdentities(jsch, authAgent);
        }
    }

    @SuppressWarnings("unchecked")
    private void updateJschWithAvailableIdentities(final JSch jsch, AndroidAuthAgent authAgent) throws JSchException {
        Map<String, byte[]> identities;
        try {
            identities = authAgent.getIdentities();
            Log.d(TAG, "updateJschWithAvailableIdentities() - identities=" + identities);
        } catch (RemoteException e) {
            throw new JSchException("Couldn't get identities from Auth Agent " + authAgent, e);
        }
        updateJschWith(jsch, identities);
    }

    private void updateJschWith(final JSch jsch, Map<String, byte[]> identities) throws JSchException {
        for (Entry<String, byte[]> i : identities.entrySet()) {
            byte[] publicKey = i.getValue();
            String name = i.getKey();
            jsch.addIdentity(new SSHAgentIdentity(androidAuthAgentProvider, publicKey, name), null);
        }
    }


}
