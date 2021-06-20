/*
 * Copyright 2015 Austin Keener, Michael Ritter, Florian Spieß, and the JDA contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dv8tion.jda.api.entities;

import net.dv8tion.jda.api.managers.StageInstanceManager;
import net.dv8tion.jda.api.requests.RestAction;

import javax.annotation.Nonnull;

public interface StageInstance extends ISnowflake
{
    Guild getGuild();
    StageChannel getChannel();

    String getTopic();
    PrivacyLevel getPrivacyLevel();

    boolean isDiscoverable();

    RestAction<Void> delete();

    StageInstanceManager getManager();

    enum PrivacyLevel
    {
        UNKNOWN(-1), PUBLIC(1), GUILD_ONLY(2);

        private final int key;

        PrivacyLevel(int key)
        {
            this.key = key;
        }

        public int getKey()
        {
            return key;
        }

        @Nonnull
        public static PrivacyLevel fromKey(int key)
        {
            for (PrivacyLevel level : values())
            {
                if (level.key == key)
                    return level;
            }
            return UNKNOWN;
        }
    }
}
