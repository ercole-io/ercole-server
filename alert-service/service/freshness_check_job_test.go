// Copyright (c) 2019 Sorint.lab S.p.A.
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package service

import (
	"testing"
	"time"

	"github.com/amreo/ercole-services/config"
	"github.com/amreo/ercole-services/utils"
	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
)

func TestFreshnessCheckJobRun_SuccessNoOldCurrentHosts(t *testing.T) {
	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()
	db := NewMockMongoDatabaseInterface(mockCtrl)
	as := NewMockAlertServiceInterface(mockCtrl)
	fcj := FreshnessCheckJob{
		TimeNow:      utils.Btc(utils.P("2019-11-05T14:02:03Z")),
		alertService: as,
		Database:     db,
		Config: config.Configuration{
			AlertService: config.AlertService{
				FreshnessCheckJob: config.FreshnessCheckJob{
					DaysThreshold: 10,
				},
			},
		},
		Log: utils.NewLogger("TEST"),
	}

	db.EXPECT().FindOldCurrentHosts(gomock.Any()).Return([]string{}, nil).Do(func(tm time.Time) {
		assert.Equal(t, utils.P("2019-10-26T14:02:03Z"), tm)
	}).Times(1)
	db.EXPECT().ExistNoDataAlertByHost(gomock.Any()).Times(0)
	as.EXPECT().ThrowNoDataAlert(gomock.Any(), gomock.Any()).Times(0)

	fcj.Run()
}

func TestFreshnessCheckJobRun_SuccessTwoOldCurrentHostsWithoutNoDataAlert(t *testing.T) {
	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()
	db := NewMockMongoDatabaseInterface(mockCtrl)
	as := NewMockAlertServiceInterface(mockCtrl)
	fcj := FreshnessCheckJob{
		TimeNow:      utils.Btc(utils.P("2019-11-05T14:02:03Z")),
		alertService: as,
		Database:     db,
		Config: config.Configuration{
			AlertService: config.AlertService{
				FreshnessCheckJob: config.FreshnessCheckJob{
					DaysThreshold: 10,
				},
			},
		},
		Log: utils.NewLogger("TEST"),
	}

	db.EXPECT().FindOldCurrentHosts(gomock.Any()).Return([]string{"pippohost", "plutohost"}, nil).Do(func(tm time.Time) {
		assert.Equal(t, utils.P("2019-10-26T14:02:03Z"), tm)
	})
	db.EXPECT().ExistNoDataAlertByHost("pippohost").Return(false, nil).Times(1)
	db.EXPECT().ExistNoDataAlertByHost("plutohost").Return(false, nil).Times(1)
	db.EXPECT().ExistNoDataAlertByHost(gomock.Any()).Times(0)

	as.EXPECT().ThrowNoDataAlert("pippohost", 10).Return(nil).Times(1)
	as.EXPECT().ThrowNoDataAlert("plutohost", 10).Return(nil).Times(1)
	as.EXPECT().ThrowNoDataAlert(gomock.Any(), gomock.Any()).Return(nil).Times(0)

	fcj.Run()
}

func TestFreshnessCheckJobRun_SuccessTwoOldCurrentHostsWithNoDataAlert(t *testing.T) {
	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()
	db := NewMockMongoDatabaseInterface(mockCtrl)
	as := NewMockAlertServiceInterface(mockCtrl)
	fcj := FreshnessCheckJob{
		TimeNow:      utils.Btc(utils.P("2019-11-05T14:02:03Z")),
		alertService: as,
		Database:     db,
		Config: config.Configuration{
			AlertService: config.AlertService{
				FreshnessCheckJob: config.FreshnessCheckJob{
					DaysThreshold: 10,
				},
			},
		},
		Log: utils.NewLogger("TEST"),
	}

	db.EXPECT().FindOldCurrentHosts(gomock.Any()).Return([]string{"pippohost", "plutohost"}, nil).Do(func(tm time.Time) {
		assert.Equal(t, utils.P("2019-10-26T14:02:03Z"), tm)
	})
	db.EXPECT().ExistNoDataAlertByHost("pippohost").Return(true, nil).Times(1)
	db.EXPECT().ExistNoDataAlertByHost("plutohost").Return(true, nil).Times(1)
	db.EXPECT().ExistNoDataAlertByHost(gomock.Any()).Times(0)

	as.EXPECT().ThrowNoDataAlert(gomock.Any(), gomock.Any()).Return(nil).Times(0)

	fcj.Run()
}
func TestFreshnessCheckJobRun_DatabaseError1(t *testing.T) {
	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()
	db := NewMockMongoDatabaseInterface(mockCtrl)
	as := NewMockAlertServiceInterface(mockCtrl)
	fcj := FreshnessCheckJob{
		TimeNow:      utils.Btc(utils.P("2019-11-05T14:02:03Z")),
		alertService: as,
		Database:     db,
		Config: config.Configuration{
			AlertService: config.AlertService{
				FreshnessCheckJob: config.FreshnessCheckJob{
					DaysThreshold: 10,
				},
			},
		},
		Log: utils.NewLogger("TEST"),
	}

	db.EXPECT().FindOldCurrentHosts(gomock.Any()).Return(nil, aerrMock).Do(func(tm time.Time) {
		assert.Equal(t, utils.P("2019-10-26T14:02:03Z"), tm)
	}).Times(1)
	db.EXPECT().ExistNoDataAlertByHost(gomock.Any()).Times(0)
	as.EXPECT().ThrowNoDataAlert(gomock.Any(), gomock.Any()).Times(0)

	fcj.Run()
}

func TestFreshnessCheckJobRun_DatabaseError2(t *testing.T) {
	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()
	db := NewMockMongoDatabaseInterface(mockCtrl)
	as := NewMockAlertServiceInterface(mockCtrl)
	fcj := FreshnessCheckJob{
		TimeNow:      utils.Btc(utils.P("2019-11-05T14:02:03Z")),
		alertService: as,
		Database:     db,
		Config: config.Configuration{
			AlertService: config.AlertService{
				FreshnessCheckJob: config.FreshnessCheckJob{
					DaysThreshold: 10,
				},
			},
		},
		Log: utils.NewLogger("TEST"),
	}

	db.EXPECT().FindOldCurrentHosts(gomock.Any()).Return([]string{"pippohost", "plutohost"}, nil).Do(func(tm time.Time) {
		assert.Equal(t, utils.P("2019-10-26T14:02:03Z"), tm)
	}).Times(1)

	db.EXPECT().ExistNoDataAlertByHost("pippohost").Return(false, aerrMock).Times(1)
	db.EXPECT().ExistNoDataAlertByHost(gomock.Any()).Times(0)

	fcj.Run()
}

func TestFreshnessCheckJobRun_AlertServiceError2(t *testing.T) {
	mockCtrl := gomock.NewController(t)
	defer mockCtrl.Finish()
	db := NewMockMongoDatabaseInterface(mockCtrl)
	as := NewMockAlertServiceInterface(mockCtrl)
	fcj := FreshnessCheckJob{
		TimeNow:      utils.Btc(utils.P("2019-11-05T14:02:03Z")),
		alertService: as,
		Database:     db,
		Config: config.Configuration{
			AlertService: config.AlertService{
				FreshnessCheckJob: config.FreshnessCheckJob{
					DaysThreshold: 10,
				},
			},
		},
		Log: utils.NewLogger("TEST"),
	}

	db.EXPECT().FindOldCurrentHosts(gomock.Any()).Return([]string{"pippohost", "plutohost"}, nil).Do(func(tm time.Time) {
		assert.Equal(t, utils.P("2019-10-26T14:02:03Z"), tm)
	}).Times(1)

	db.EXPECT().ExistNoDataAlertByHost("pippohost").Return(false, nil).Times(1)
	db.EXPECT().ExistNoDataAlertByHost(gomock.Any()).Times(0)
	as.EXPECT().ThrowNoDataAlert("pippohost", 10).Return(aerrMock).Times(1)
	as.EXPECT().ThrowNoDataAlert(gomock.Any(), gomock.Any()).Return(nil).Times(0)

	fcj.Run()
}