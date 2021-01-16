/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.sponge.service.model.persisted;

import me.lucko.luckperms.common.cache.BufferedRequest;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.ProxyFactory;
import me.lucko.luckperms.sponge.service.model.LPProxiedSubject;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.calculated.CalculatedSubject;
import me.lucko.luckperms.sponge.service.model.calculated.CalculatedSubjectData;
import me.lucko.luckperms.sponge.service.model.calculated.MonitoredSubjectData;

import net.luckperms.api.model.data.DataType;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * A simple persistable Subject implementation
 */
public class PersistedSubject extends CalculatedSubject implements LPSubject {
    private final LuckPermsService service;

    /**
     * The subjects identifier
     */
    private final String identifier;

    /**
     * The parent collection
     */
    private final PersistedCollection parentCollection;

    // subject data instances
    private final PersistedSubjectData subjectData;
    private final CalculatedSubjectData transientSubjectData;

    private LPProxiedSubject spongeSubject = null;

    /**
     * The save buffer instance for saving changes to disk
     */
    private final SaveBuffer saveBuffer;

    /**
     * If a save is pending for this subject
     */
    private boolean pendingSave = false;

    public PersistedSubject(LuckPermsService service, PersistedCollection parentCollection, String identifier) {
        super(service.getPlugin());
        this.service = service;
        this.parentCollection = parentCollection;
        this.identifier = identifier;

        this.subjectData = new PersistedSubjectData(this, DataType.NORMAL, service) {
            @Override
            protected void onUpdate(boolean success) {
                super.onUpdate(success);
                if (success) {
                    PersistedSubject.this.service.fireUpdateEvent(this);
                }
            }
        };
        this.transientSubjectData = new MonitoredSubjectData(this, DataType.TRANSIENT, service) {
            @Override
            protected void onUpdate(boolean success) {
                if (success) {
                    PersistedSubject.this.service.fireUpdateEvent(this);
                }
            }
        };

        this.saveBuffer = new SaveBuffer(service.getPlugin());
    }

    /**
     * Loads data into this {@link PersistedSubject} from the given
     * {@link SubjectDataContainer} container
     *
     * @param container the container to load from
     */
    public void loadData(SubjectDataContainer container) {
        if (this.pendingSave) {
            return;
        }

        this.subjectData.setSave(false);
        container.applyToData(this.subjectData);
        this.subjectData.setSave(true);
    }

    /**
     * Requests that this subjects data is saved to disk
     */
    public void save() {
        this.pendingSave = true;
        this.saveBuffer.request();
    }

    void doSave() {
        try {
            this.service.getStorage().saveToFile(PersistedSubject.this);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            this.pendingSave = false;
        }
    }

    @Override
    public LPProxiedSubject sponge() {
        if (this.spongeSubject == null) {
            this.spongeSubject = ProxyFactory.toSponge(this);
        }
        return this.spongeSubject;
    }

    @Override
    public String getIdentifier() {
        return this.identifier;
    }

    @Override
    public LuckPermsService getService() {
        return this.service;
    }

    @Override
    public PersistedCollection getParentCollection() {
        return this.parentCollection;
    }

    @Override
    public PersistedSubjectData getSubjectData() {
        return this.subjectData;
    }

    @Override
    public CalculatedSubjectData getTransientSubjectData() {
        return this.transientSubjectData;
    }

    private final class SaveBuffer extends BufferedRequest<Void> {
        public SaveBuffer(LuckPermsPlugin plugin) {
            super(1, TimeUnit.SECONDS, plugin.getBootstrap().getScheduler());
        }

        @Override
        protected Void perform() {
            doSave();
            return null;
        }
    }
}
