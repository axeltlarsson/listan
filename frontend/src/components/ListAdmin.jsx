import React from 'react'
import Heading from './Heading'
import AddList from '../containers/AddList'
import ListsContainer from '../containers/ListsContainer'

const ListAdmin = ({}) => (
  <div id="list-admin">
    <ListsContainer />
    <Heading id="add-lists-header" value="Lägg till lista" />
    <AddList />
  </div>

)
export default ListAdmin

