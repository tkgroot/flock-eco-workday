import List from "@material-ui/core/List"
import ListItem from "@material-ui/core/ListItem"
import React from "react"
import ListItemText from "@material-ui/core/ListItemText"
import ListItemIcon from "@material-ui/core/ListItemIcon"

import AssignmentIcon from "@material-ui/icons/Assignment"
import HolidayIcon from "@material-ui/icons/WbSunny"
import HealingIcon from "@material-ui/icons/Healing"
import EventIcon from "@material-ui/icons/CalendarToday"
import UserIcon from "@material-ui/icons/Person"
import ClientIcon from "@material-ui/icons/Business"

import Drawer from "@material-ui/core/Drawer"
import makeStyles from "@material-ui/core/styles/makeStyles"
import {withRouter} from "react-router-dom"

const useStyles = makeStyles({
  head: {
    height: 60,
  },
  list: {
    width: 250,
  },
  fullList: {
    width: "auto",
  },
})

export const ApplicationDrawer = withRouter(({open, onClose, history}) => {
  const classes = useStyles()

  const handleClose = event => {
    if (event.type === "keydown" && (event.key === "Tab" || event.key === "Shift")) {
      return
    }
    if (onClose) {
      onClose()
    }
  }

  const handleClickItem = item => () => {
    history.push(item.url)
  }

  const items = [
    {
      name: "Clients",
      icon: ClientIcon,
      url: "/clients",
    },
    {
      name: "Assignments",
      icon: AssignmentIcon,
      url: "/assignments",
    },
    {
      name: "Holidays",
      icon: HolidayIcon,
      url: "/holidays",
    },
    {
      name: "Sickday",
      icon: HealingIcon,
      url: "/sickdays",
    },
    {
      name: "Events",
      icon: EventIcon,
      url: "/events",
    },
    {
      name: "User",
      icon: UserIcon,
      url: "/users",
    },
    {
      name: "Person",
      icon: UserIcon,
      url: "/person",
    },
  ]

  const sideList = () => (
    <div
      className={classes.list}
      role="presentation"
      onClick={handleClose}
      onKeyDown={handleClose}
    >
      <List>
        {items.map(item => (
          <ListItem
            button
            key={`menu-item-${item.name}`}
            onClick={handleClickItem(item)}
          >
            <ListItemIcon>{React.createElement(item.icon)}</ListItemIcon>
            <ListItemText primary={item.name} />
          </ListItem>
        ))}
      </List>
    </div>
  )
  return (
    <Drawer open={open} onClose={handleClose}>
      <div className={classes.head} />
      {sideList()}
    </Drawer>
  )
})
