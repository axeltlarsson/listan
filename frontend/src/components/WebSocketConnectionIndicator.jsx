import React from 'react'
import {Tooltip, Icon, Spinner} from 'react-mdl'


const WebSocketConnectionIndicator = (
  {
    isReady,
    isPending
  }
) => {
  return (
    <div id="ws-conn-indicator">
    {!isReady && !isPending &&
      <Tooltip label="Offline" large>
        <Icon name="cloud_off" />
      </Tooltip>
    }
    {!isReady && isPending &&
      <Tooltip label="Försöker etablera anslutning" >
        <Spinner />
      </Tooltip>
    }
    </div>
  )
}

export default WebSocketConnectionIndicator

