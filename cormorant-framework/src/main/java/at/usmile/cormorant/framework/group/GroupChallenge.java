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

public class GroupChallenge {
    private int pin;
    private String jabberId;

    public GroupChallenge(int pin, String jabberId) {
        this.pin = pin;
        this.jabberId = jabberId;
    }

    public int getPin() {
        return pin;
    }

    public String getJabberId() {
        return jabberId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GroupChallenge that = (GroupChallenge) o;

        if (pin != that.pin) return false;
        return jabberId.equals(that.jabberId);

    }

    @Override
    public int hashCode() {
        int result = pin;
        result = 31 * result + jabberId.hashCode();
        return result;
    }
}
