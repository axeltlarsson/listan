import React from 'react'
import Item from './Item'
import ReactCSSTransitionGroup from 'react-addons-css-transition-group'

const ItemList = ({
  items,
  onItemClick,  /* when item is clicked (toggle it prob. )*/
  onEditClick,  /* when edit button clicked (display edit mode prob.) */
  onDeleteClick,
  onItemChange
}) => (
  <div id="item-list">
    <ReactCSSTransitionGroup
      transitionName="ex"
      transitionEnterTimeout={200}
      transitionLeaveTimeout={200}>
      {items.map((item) =>
        <Item
          key={item.uuid}
          {...item}
          onClick={() => onItemClick(item.uuid, item.completed)}
          onEditClick={() => onEditClick(item.uuid)}
          onDeleteClick={() => onDeleteClick(item.uuid)}
          onItemChange={onItemChange}
        />
      )}
    </ReactCSSTransitionGroup>
  </div>
)

export default ItemList
