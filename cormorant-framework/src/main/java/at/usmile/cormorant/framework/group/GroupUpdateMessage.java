/**
 * Copyright 2016 - 2017
 *
 * Daniel Hintze <daniel.hintze@fhdw.de>
 * Sebastian Scholz <sebastian.scholz@fhdw.de>
 * Rainhard D. Findling <rainhard.findling@fh-hagenberg.at>
 * Muhammad Muaaz <muhammad.muaaz@usmile.at>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.usmile.cormorant.framework.group;

import java.util.List;

import at.usmile.cormorant.framework.messaging.CormorantMessage;

public class GroupUpdateMessage extends CormorantMessage {
    private List<TrustedDevice> group;

    public GroupUpdateMessage(List<TrustedDevice> group) {
        super(TYPE.GROUP, CLASS.GROUP_UPDATE);
        this.group = group;
    }

    public List<TrustedDevice> getGroup() {
        return group;
    }

    @Override
    public String toString() {
        return "GroupUpdateMessage{" +
                "group=" + group +
                "} " + super.toString();
    }
}
