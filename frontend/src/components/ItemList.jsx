import React from 'react'
import Item from './Item'
import {
  CSSTransition,
  TransitionGroup,
} from 'react-transition-group';

const ItemList = ({
  items,
  onItemClick,  /* when item is clicked (toggle it prob. )*/
  onEditClick,  /* when edit button clicked (display edit mode prob.) */
  onDeleteClick,
  onItemChange
}) => (
  <div id="item-list">
      <TransitionGroup className="transition-group" >
      {items.map((item) =>
        <CSSTransition
          key={item.uuid}
          timeout={500}
          classNames="fade"
        >
        <Item
          key={item.uuid}
          {...item}
          onClick={() => onItemClick(item.uuid, item.completed)}
          onEditClick={() => onEditClick(item.uuid)}
          onDeleteClick={() => onDeleteClick(item.uuid)}
          onItemChange={onItemChange}
        />
        </CSSTransition>
      )}
    </TransitionGroup>
  </div>
)

export default ItemList
