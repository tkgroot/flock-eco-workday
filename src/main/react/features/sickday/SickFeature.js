import React, {useContext, useEffect, useState} from "react"

import {makeStyles} from "@material-ui/core/styles"
import Grid from "@material-ui/core/Grid"
import {SickdayDialog} from "./SickdayDialog"
import {SickdayList} from "./SickdayList"
import {PersonSelector} from "../../components/selector"
import {ApplicationContext} from "../../application/ApplicationContext"
import {AddActionFab} from "../../components/FabButtons"
import {PersonService} from "../person/PersonService"

const useStyles = makeStyles({
  root: {
    padding: 20,
  },
  fab: {
    position: "absolute",
    bottom: "25px",
    right: "25px",
  },
})

/**
 * @return {null}
 */
export function SickdayFeature() {
  const classes = useStyles()

  const [reload, setReload] = useState(false)
  const [open, setOpen] = useState(false)
  const [value, setValue] = useState(null)
  const [personCode, setPersonCode] = useState("")
  const [persons, setPersons] = useState([]) // eslint-disable-line no-unused-vars
  const {authorities, user} = useContext(ApplicationContext)

  function isSuperUser() {
    return authorities && authorities.includes("SickdayAuthority.ADMIN")
  }

  useEffect(() => {
    if (isSuperUser()) {
      PersonService.getAll().then(it => setPersons(it))
    }
  }, [authorities, user])

  function handleCompleteDialog() {
    setReload(reload)
    setOpen(false)
    setValue(null)
  }

  function handleClickAdd() {
    setValue(null)
    setOpen(true)
  }

  function handleClickRow(item) {
    setValue(item)

    setOpen(true)
  }

  function handlePersonChangeByCode(code) {
    setPersonCode(code)
  }

  return (
    <div className={classes.root}>
      <Grid container spacing={1}>
        <Grid item xs={12}>
          {isSuperUser() && <PersonSelector onChange={handlePersonChangeByCode} />}
        </Grid>
        <Grid item xs={12}>
          <SickdayList
            personCode={personCode}
            onClickRow={handleClickRow}
            refresh={reload}
          />
        </Grid>
      </Grid>
      <SickdayDialog
        open={open}
        personCode={personCode}
        value={value}
        onClose={handleCompleteDialog}
      />

      <AddActionFab color="primary" onClick={handleClickAdd} />
    </div>
  )
}

SickdayFeature.propTypes = {}
